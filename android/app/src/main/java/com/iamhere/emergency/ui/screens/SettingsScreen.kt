package com.iamhere.emergency.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.data.api.NetworkClient
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*
import com.iamhere.emergency.update.AppUpdateManager
import com.iamhere.emergency.update.UpdateState

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val manager = remember { EmergencyManager.getInstance() }
    val updateManager = remember { AppUpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(Unit) {
        // Automatically check for newer builds silently when entering settings
        updateManager.checkForUpdates(silent = true)
    }

    val activeVictimPhone by manager.userPhoneFlow.collectAsState()
    val activeVictimName by manager.userNameFlow.collectAsState()

    var name by remember { mutableStateOf(manager.userName) }
    var phone by remember { mutableStateOf(manager.userPhone) }
    var bloodGroup by remember { mutableStateOf(manager.bloodGroup) }
    var medicalNotes by remember { mutableStateOf(manager.medicalNotes) }
    var cancelPin by remember { mutableStateOf(manager.cancelPin) }
    var duressPin by remember { mutableStateOf(manager.duressPin) }
    var serverUrl by remember { mutableStateOf(NetworkClient.baseUrl) }

    var shakeEnabled by remember { mutableStateOf(manager.isShakeDetectionEnabled) }
    var sensitivity by remember { mutableStateOf(manager.shakeSensitivity) }
    var impactEnabled by remember { mutableStateOf(manager.isImpactDetectionEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SAFETY PROFILE & SECURITY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextGlassPrimary
                )
                Text(
                    text = "Emergency identity, PINs, and auto-updates",
                    fontSize = 12.sp,
                    color = TextGlassSecondary
                )
            }

            // Quick Update Icon Button in Settings Header
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF222329))
                    .border(
                        width = 1.dp,
                        color = if (updateState is UpdateState.Available) Color.White else Color(0x35FFFFFF),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        when (val state = updateState) {
                            is UpdateState.Available -> updateManager.startDownload(state.info)
                            is UpdateState.ReadyToInstall -> updateManager.installApk(state.apkFile)
                            is UpdateState.Downloading -> { /* In progress */ }
                            else -> updateManager.checkForUpdates(silent = false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (updateState is UpdateState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Check for Updates",
                        tint = if (updateState is UpdateState.Available) Color.White else TextGlassSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }


        // Section 0: Active Victim Identity Card (Phone Login Identity)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222329))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Active Victim Protection", color = TextGlassPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Distress telemetry runs specifically for this number", color = TextGlassSecondary, fontSize = 11.sp)
                        }
                    }
                    GlassPill("PROTECTED", tintColor = Color.White)
                }

                HorizontalDivider(color = Color(0x18FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeVictimName.isNotBlank()) activeVictimName else name,
                            color = TextGlassPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (activeVictimPhone.isNotBlank()) activeVictimPhone else phone,
                            color = EmergencyRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    GlassButton(
                        onClick = {
                            manager.logout()
                            Toast.makeText(context, "Logged out of victim session", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color(0x18FFFFFF),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch Phone", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 1: User Identity & Medical Notes
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222329))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text("Profile & Medical Details", color = TextGlassPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                GlassInputField(value = name, onValueChange = { name = it }, placeholder = "Full Legal Name")
                GlassInputField(value = phone, onValueChange = { phone = it }, placeholder = "Your Mobile (+91...)")
                GlassInputField(value = bloodGroup, onValueChange = { bloodGroup = it }, placeholder = "Blood Group (e.g. B+, O+)")
                GlassInputField(value = medicalNotes, onValueChange = { medicalNotes = it }, placeholder = "Medical Notes (Allergies, Conditions)")
            }
        }

        // Section 2: Anti-Tamper Security PINs
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222329))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                    }
                    Text("Anti-Tamper PINs", color = TextGlassPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Master PIN deactivates distress alerts safely. Duress PIN visually turns off the screen while covertly broadcasting distress to primary contacts.",
                    color = TextGlassSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master PIN", fontSize = 11.sp, color = TextGlassMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        GlassInputField(
                            value = cancelPin,
                            onValueChange = { if (it.length <= 4) cancelPin = it },
                            placeholder = "1234"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Secret Duress PIN", fontSize = 11.sp, color = EmergencyRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        GlassInputField(
                            value = duressPin,
                            onValueChange = { if (it.length <= 4) duressPin = it },
                            placeholder = "9999"
                        )
                    }
                }
            }
        }

        // Section 3: Motion & Hardware Sensor Triggers
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222329))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text("Sensor Triggers", color = TextGlassPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Shake Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vigorous Shake to SOS", color = TextGlassPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Triggers SOS after 3 reversals in 800ms", color = TextGlassSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = shakeEnabled,
                        onCheckedChange = {
                            shakeEnabled = it
                            manager.updateShakeSettings(shakeEnabled, sensitivity, impactEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmergencyRed,
                            uncheckedThumbColor = TextGlassMuted,
                            uncheckedTrackColor = Color(0x20FFFFFF)
                        )
                    )
                }

                if (shakeEnabled) {
                    Text("SENSITIVITY THRESHOLD", color = TextGlassMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            com.iamhere.emergency.sensor.ShakeSensitivity.LOW to "Low (3.2g)",
                            com.iamhere.emergency.sensor.ShakeSensitivity.MEDIUM to "Medium (2.7g)",
                            com.iamhere.emergency.sensor.ShakeSensitivity.HIGH to "High (2.2g)"
                        ).forEach { (level, label) ->
                            val isSelected = sensitivity == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF222329) else Color(0x10FFFFFF))
                                    .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable {
                                        sensitivity = level
                                        manager.updateShakeSettings(shakeEnabled, level, impactEnabled)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextGlassSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0x15FFFFFF))

                // Fall Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fall & Freefall Impact", color = TextGlassPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Detects sudden drops followed by hard strike", color = TextGlassSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = impactEnabled,
                        onCheckedChange = {
                            impactEnabled = it
                            manager.updateShakeSettings(shakeEnabled, sensitivity, impactEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmergencyRed,
                            uncheckedThumbColor = TextGlassMuted,
                            uncheckedTrackColor = Color(0x20FFFFFF)
                        )
                    )
                }

                GlassButton(
                    onClick = { manager.onMotionTriggered("Hardware Shake Simulation") },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    backgroundColor = Color(0x18FFFFFF),
                    borderColor = Color(0x35FFFFFF),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Simulate Shake Countdown (10s)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Section 4: Backend Cloud Connection
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Emergency Cloud Gateway", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                GlassInputField(value = serverUrl, onValueChange = { serverUrl = it }, placeholder = "http://10.0.2.2:3000")
            }
        }

        // Section 5: App Build & In-App Auto-Update
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF2121317)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222329))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text("App Build & Updates", color = TextGlassPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Installed: v${updateManager.currentVersionName} (Build ${updateManager.currentVersionCode})", color = TextGlassSecondary, fontSize = 11.sp)
                        }
                    }

                    when (val state = updateState) {
                        is UpdateState.Checking -> GlassPill("CHECKING", tintColor = TextGlassMuted)
                        is UpdateState.Available -> GlassPill("NEW v${state.info.versionName}", tintColor = Color.White)
                        is UpdateState.Downloading -> GlassPill("${state.progressPercent}%", tintColor = Color.White)
                        is UpdateState.ReadyToInstall -> GlassPill("READY", tintColor = Color.White)
                        is UpdateState.UpToDate -> GlassPill("LATEST", tintColor = TextGlassSecondary)
                        is UpdateState.Error -> GlassPill("OFFLINE", tintColor = TextGlassMuted)
                        is UpdateState.Idle -> GlassPill("READY", tintColor = TextGlassMuted)
                    }
                }

                HorizontalDivider(color = Color(0x18FFFFFF))

                // Status & Release Notes
                when (val state = updateState) {
                    is UpdateState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text("Querying build server for latest release...", color = TextGlassSecondary, fontSize = 12.sp)
                        }
                    }
                    is UpdateState.Available -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Server Release: v${state.info.versionName} (Build ${state.info.versionCode})",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (state.info.releaseNotes.isNotBlank()) {
                                Text(
                                    state.info.releaseNotes,
                                    color = TextGlassSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            val mbSize = if (state.info.fileSizeBytes > 0) {
                                String.format(java.util.Locale.US, "%.1f MB", state.info.fileSizeBytes / (1024f * 1024f))
                            } else {
                                "Direct APK"
                            }
                            Text("Download payload: $mbSize", color = TextGlassMuted, fontSize = 11.sp)
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Streaming latest APK build...", color = TextGlassPrimary, fontSize = 12.sp)
                                Text("${state.progressPercent}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color.White,
                                trackColor = Color(0x25FFFFFF)
                            )
                            val downloadedMb = String.format(java.util.Locale.US, "%.1f", state.downloadedBytes / (1024f * 1024f))
                            val totalMb = if (state.totalBytes > 0) {
                                String.format(java.util.Locale.US, "%.1f MB", state.totalBytes / (1024f * 1024f))
                            } else {
                                "streaming"
                            }
                            Text("$downloadedMb MB / $totalMb", color = TextGlassMuted, fontSize = 10.sp)
                        }
                    }
                    is UpdateState.ReadyToInstall -> {
                        Text(
                            "Download completed. Tap 'Launch Installer' below to apply the update.",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    is UpdateState.UpToDate -> {
                        Text(
                            "Your device is running the current build. You can re-download the latest debug binary anytime to test newly compiled features without ADB.",
                            color = TextGlassSecondary,
                            fontSize = 12.sp
                        )
                    }
                    is UpdateState.Error -> {
                        Text(
                            state.message,
                            color = EmergencyRed,
                            fontSize = 12.sp
                        )
                    }
                    is UpdateState.Idle -> {
                        Text(
                            "Check for newly compiled builds and update directly over Wi-Fi/USB without manual adb commands.",
                            color = TextGlassSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Permission Warning Banner
                if (!updateManager.canRequestPackageInstalls()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x25E53935))
                            .border(1.dp, EmergencyRed, RoundedCornerShape(12.dp))
                            .clickable { updateManager.requestInstallUnknownAppsPermission() }
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                            Text(
                                "Install permission needed: Tap to allow I AM HERE to update",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (val state = updateState) {
                        is UpdateState.Available -> {
                            GlassButton(
                                onClick = { updateManager.startDownload(state.info) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                backgroundColor = Color(0xFF222329),
                                borderColor = Color.White,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download & Install (v${state.info.versionName})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            GlassButton(
                                onClick = { updateManager.installApk(state.apkFile) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                backgroundColor = Color(0xFF222329),
                                borderColor = Color.White,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Package Installer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateState.Downloading -> {
                            GlassButton(
                                onClick = { /* downloading */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                backgroundColor = Color(0x18FFFFFF),
                                borderColor = Color(0x35FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading ${state.progressPercent}%...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        else -> {
                            GlassButton(
                                onClick = { updateManager.checkForUpdates(silent = false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                backgroundColor = Color(0x18FFFFFF),
                                borderColor = Color(0x35FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            GlassButton(
                                onClick = {
                                    val info = updateManager.latestVersionInfo
                                    updateManager.startDownload(info)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                backgroundColor = Color(0xFF222329),
                                borderColor = Color(0x40FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reinstall Build", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }


        // Save Settings Action Button
        GlassButton(
            onClick = {
                manager.userName = name
                manager.userPhone = phone
                manager.bloodGroup = bloodGroup
                manager.medicalNotes = medicalNotes
                manager.cancelPin = cancelPin
                manager.duressPin = duressPin
                NetworkClient.baseUrl = serverUrl
                Toast.makeText(context, "Safety settings saved successfully", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            backgroundColor = Color(0xFF222329),
            borderColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextGlassMuted, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextGlassPrimary,
            unfocusedTextColor = TextGlassPrimary,
            focusedContainerColor = Color(0x18FFFFFF),
            unfocusedContainerColor = Color(0x0CFFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
