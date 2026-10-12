package com.iamhere.emergency.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.iamhere.emergency.data.api.NetworkClient
import com.iamhere.emergency.data.model.AppVersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val currentVersion: String, val lastChecked: Long = System.currentTimeMillis()) : UpdateState()
    data class Available(val info: AppVersionInfo, val isHigher: Boolean) : UpdateState()
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val versionInfo: AppVersionInfo?) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    var latestVersionInfo: AppVersionInfo? = null
        private set

    val currentVersionCode: Long
        get() {
            return try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (e: Exception) {
                1L
            }
        }

    val currentVersionName: String
        get() {
            return try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                pInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
        }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallUnknownAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (!silent) {
            _updateState.value = UpdateState.Checking
        }
        scope.launch {
            try {
                val response = NetworkClient.api.getAppVersion()
                if (response.isSuccessful && response.body() != null) {
                    val info = response.body()!!
                    latestVersionInfo = info
                    val isHigher = info.versionCode > currentVersionCode
                    _updateState.value = if (isHigher) {
                        UpdateState.Available(info, isHigher = true)
                    } else {
                        // Keep info available so user can force reinstall/test current or latest build
                        UpdateState.UpToDate(currentVersionName)
                    }
                } else {
                    if (!silent) {
                        _updateState.value = UpdateState.Error("Server check failed (HTTP ${response.code()})")
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _updateState.value = UpdateState.Error("Connection error: ${e.localizedMessage ?: "Cannot reach update server"}")
                }
            }
        }
    }

    fun startDownload(info: AppVersionInfo? = latestVersionInfo) {
        scope.launch {
            try {
                val apkPath = info?.apkUrl ?: "/api/app/latest.apk"
                val resolvedUrl = if (apkPath.startsWith("http://") || apkPath.startsWith("https://")) {
                    apkPath
                } else {
                    "${NetworkClient.baseUrl.trimEnd('/')}/${apkPath.trimStart('/')}"
                }

                _updateState.value = UpdateState.Downloading(progressPercent = 0, downloadedBytes = 0, totalBytes = info?.fileSizeBytes ?: 0)

                val request = Request.Builder().url(resolvedUrl).build()
                val response = withContext(Dispatchers.IO) {
                    downloadClient.newCall(request).execute()
                }

                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("Failed to download APK (HTTP ${response.code})")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _updateState.value = UpdateState.Error("Empty response body from update server")
                    return@launch
                }

                val contentLength = body.contentLength().takeIf { it > 0 } ?: (info?.fileSizeBytes?.takeIf { it > 0 } ?: -1L)
                val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
                val targetApk = File(updatesDir, "iamhere-latest.apk")

                if (targetApk.exists()) {
                    targetApk.delete()
                }

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(targetApk)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0
                    var lastNotifiedPercent = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val percent = if (contentLength > 0) {
                            ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            50 // Indeterminate progress
                        }

                        if (percent != lastNotifiedPercent || percent == 100) {
                            lastNotifiedPercent = percent
                            _updateState.value = UpdateState.Downloading(
                                progressPercent = percent,
                                downloadedBytes = totalRead,
                                totalBytes = if (contentLength > 0) contentLength else totalRead
                            )
                        }
                    }

                    outputStream.flush()
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }

                _updateState.value = UpdateState.ReadyToInstall(targetApk, info)

                // Trigger package installation prompt
                installApk(targetApk)

            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Download interrupted: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("APK file not found on disk")
                return
            }

            if (!canRequestPackageInstalls()) {
                requestInstallUnknownAppsPermission()
                return
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Install trigger failed: ${e.localizedMessage ?: "Permission or package parsing error"}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    companion object {
        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
