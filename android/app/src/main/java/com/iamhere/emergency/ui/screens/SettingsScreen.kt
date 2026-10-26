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
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safety Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Emergency identity, PINs, and auto-updates",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )
            }

            // Quick Update Icon Button in Settings Header
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SoftBlush)
                    .border(
                        width = 1.dp,
                        color = if (updateState is UpdateState.Available) CoralPrimary else SoftBlushBorder,
                        shape = CircleShape
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
                        color = CoralPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Check for Updates",
                        tint = CoralPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section 0: Active Victim Identity Card (Phone Login Identity)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SoftBlush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Active Protection Identity", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Distress telemetry linked to this phone", color = TextSecondaryLight, fontSize = 11.sp)
                        }
                    }
                    GlassPill("PROTECTED", tintColor = StatusSafeGreen)
                }

                HorizontalDivider(color = BorderSubtleLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeVictimName.isNotBlank()) activeVictimName else name,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (activeVictimPhone.isNotBlank()) activeVictimPhone else phone,
                            color = CoralPrimary,
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
                        backgroundColor = SurfaceLightNeutral,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch Phone", color = TextPrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 1: User Identity & Medical Notes
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SoftBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text("Profile & Medical Details", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SoftBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text("Anti-Tamper PINs", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Master PIN deactivates distress alerts safely. Duress PIN visually turns off the screen while covertly broadcasting distress to primary contacts.",
                    color = TextSecondaryLight,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master PIN", fontSize = 11.sp, color = TextSecondaryLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
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
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SoftBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text("Sensor Triggers", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Shake Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vigorous Shake to SOS", color = TextPrimaryLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Triggers SOS after 3 reversals in 800ms", color = TextSecondaryLight, fontSize = 11.sp)
                    }
                    Switch(
                        checked = shakeEnabled,
                        onCheckedChange = {
                            shakeEnabled = it
                            manager.updateShakeSettings(shakeEnabled, sensitivity, impactEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralPrimary,
                            uncheckedThumbColor = TextMutedLight,
                            uncheckedTrackColor = SurfaceLightNeutral
                        )
                    )
                }

                if (shakeEnabled) {
                    Text("SENSITIVITY THRESHOLD", color = TextSecondaryLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                                    .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                    .border(1.dp, if (isSelected) CoralPrimary else BorderSubtleLight, RoundedCornerShape(12.dp))
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
                                    color = if (isSelected) Color.White else TextPrimaryLight,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtleLight)

                // Fall Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fall & Freefall Impact", color = TextPrimaryLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Detects sudden drops followed by hard strike", color = TextSecondaryLight, fontSize = 11.sp)
                    }
                    Switch(
                        checked = impactEnabled,
                        onCheckedChange = {
                            impactEnabled = it
                            manager.updateShakeSettings(shakeEnabled, sensitivity, impactEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralPrimary,
                            uncheckedThumbColor = TextMutedLight,
                            uncheckedTrackColor = SurfaceLightNeutral
                        )
                    )
                }

                GlassButton(
                    onClick = { manager.onMotionTriggered("Hardware Shake Simulation") },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    backgroundColor = SurfaceLightNeutral,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Simulate Shake Countdown (10s)", color = TextPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Section 4: Backend Cloud Connection
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Emergency Cloud Gateway", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                GlassInputField(value = serverUrl, onValueChange = { serverUrl = it }, placeholder = "http://10.0.2.2:3000")
            }
        }

        // Section 5: App Build & In-App Auto-Update
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SurfaceWhite,
            shadowElevation = 2.dp
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SoftBlush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text("App Build & Updates", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Installed: v${updateManager.currentVersionName} (Build ${updateManager.currentVersionCode})", color = TextSecondaryLight, fontSize = 11.sp)
                        }
                    }

                    when (val state = updateState) {
                        is UpdateState.Checking -> GlassPill("CHECKING", tintColor = TextSecondaryLight)
                        is UpdateState.Available -> GlassPill("NEW v${state.info.versionName}", tintColor = CoralPrimary)
                        is UpdateState.Downloading -> GlassPill("${state.progressPercent}%", tintColor = CoralPrimary)
                        is UpdateState.ReadyToInstall -> GlassPill("READY", tintColor = StatusSafeGreen)
                        is UpdateState.UpToDate -> GlassPill("LATEST", tintColor = StatusSafeGreen)
                        is UpdateState.Error -> GlassPill("OFFLINE", tintColor = EmergencyRed)
                        is UpdateState.Idle -> GlassPill("READY", tintColor = TextSecondaryLight)
                    }
                }

                HorizontalDivider(color = BorderSubtleLight)

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
                                color = CoralPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Querying build server for latest release...", color = TextSecondaryLight, fontSize = 12.sp)
                        }
                    }
                    is UpdateState.Available -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Server Release: v${state.info.versionName} (Build ${state.info.versionCode})",
                                color = TextPrimaryLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (state.info.releaseNotes.isNotBlank()) {
                                Text(
                                    state.info.releaseNotes,
                                    color = TextSecondaryLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            val mbSize = if (state.info.fileSizeBytes > 0) {
                                String.format(java.util.Locale.US, "%.1f MB", state.info.fileSizeBytes / (1024f * 1024f))
                            } else {
                                "Direct APK"
                            }
                            Text("Download payload: $mbSize", color = TextMutedLight, fontSize = 11.sp)
                        }
                    }
                    is UpdateState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Streaming latest APK build...", color = TextPrimaryLight, fontSize = 12.sp)
                                Text("${state.progressPercent}%", color = CoralPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CoralPrimary,
                                trackColor = SoftBlush
                            )
                            val downloadedMb = String.format(java.util.Locale.US, "%.1f", state.downloadedBytes / (1024f * 1024f))
                            val totalMb = if (state.totalBytes > 0) {
                                String.format(java.util.Locale.US, "%.1f MB", state.totalBytes / (1024f * 1024f))
                            } else {
                                "streaming"
                            }
                            Text("$downloadedMb MB / $totalMb", color = TextMutedLight, fontSize = 10.sp)
                        }
                    }
                    is UpdateState.ReadyToInstall -> {
                        Text(
                            "Download completed. Tap 'Launch Installer' below to apply the update.",
                            color = StatusSafeGreen,
                            fontSize = 12.sp
                        )
                    }
                    is UpdateState.UpToDate -> {
                        Text(
                            "Your device is running the current build. You can re-download the latest debug binary anytime to test newly compiled features without ADB.",
                            color = TextSecondaryLight,
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
                            color = TextSecondaryLight,
                            fontSize = 12.sp
                        )
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
                                    .fillMaxWidth()
                                    .height(46.dp),
                                backgroundColor = CoralPrimary,
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
                                    .fillMaxWidth()
                                    .height(46.dp),
                                backgroundColor = StatusSafeGreen,
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
                                    .fillMaxWidth()
                                    .height(46.dp),
                                backgroundColor = SurfaceLightNeutral,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CoralPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading ${state.progressPercent}%...", color = TextPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        else -> {
                            GlassButton(
                                onClick = { updateManager.checkForUpdates(silent = false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                backgroundColor = SurfaceLightNeutral,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimaryLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", color = TextPrimaryLight, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            GlassButton(
                                onClick = {
                                    val info = updateManager.latestVersionInfo
                                    updateManager.startDownload(info)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                backgroundColor = SoftBlush,
                                borderColor = SoftBlushBorder,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reinstall Build", color = CoralPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            backgroundColor = CoralPrimary,
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
        placeholder = { Text(placeholder, color = TextMutedLight, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoralPrimary,
            unfocusedBorderColor = BorderSubtleLight,
            focusedTextColor = TextPrimaryLight,
            unfocusedTextColor = TextPrimaryLight,
            focusedContainerColor = SurfaceLightNeutral,
            unfocusedContainerColor = SurfaceLightNeutral
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
