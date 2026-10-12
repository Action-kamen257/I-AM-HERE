package com.iamhere.emergency.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iamhere.emergency.data.api.NetworkClient
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DrillScreen(onStartDrill: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { EmergencyManager.getInstance() }
    val scope = rememberCoroutineScope()

    var isCheckingNetwork by remember { mutableStateOf(false) }
    var networkStatus by remember { mutableStateOf<Boolean?>(null) }

    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasSms = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    LaunchedEffect(Unit) {
        scope.launch {
            isCheckingNetwork = true
            try {
                val res = withContext(Dispatchers.IO) {
                    NetworkClient.api.getSosStatus("test_health_probe")
                }
                networkStatus = res.code() != 500
            } catch (e: Exception) {
                networkStatus = false
            } finally {
                isCheckingNetwork = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "SAFETY DRILL & TELEMETRY",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextGlassPrimary
            )
            Text(
                text = "Rehearse emergency protocols & monitor sensor readiness",
                fontSize = 12.sp,
                color = TextGlassSecondary
            )
        }

        // Hardware Readiness Glass Card
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF222329))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Hardware Readiness",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGlassPrimary
                        )
                    }

                    GlassPill(
                        text = if (hasFineLocation && hasSms) "ALL PASS" else "CHECK",
                        tintColor = Color.White
                    )
                }

                HorizontalDivider(color = Color(0x15FFFFFF))

                DiagnosticGlassRow(
                    icon = Icons.Default.LocationOn,
                    label = "High-Accuracy GPS",
                    isPass = hasFineLocation,
                    passText = "Active",
                    failText = "Missing"
                )

                DiagnosticGlassRow(
                    icon = Icons.Default.Sms,
                    label = "Emergency SMS Engine",
                    isPass = hasSms,
                    passText = "Direct SMS",
                    failText = "Fallback"
                )

                DiagnosticGlassRow(
                    icon = Icons.Default.Notifications,
                    label = "High-Priority Notification Channel",
                    isPass = hasNotifications,
                    passText = "Armed",
                    failText = "Blocked"
                )

                DiagnosticGlassRow(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "OEM Battery Saver Exemption",
                    isPass = isIgnoringBatteryOptimizations,
                    passText = "Exempted",
                    failText = "Restricted"
                )

                DiagnosticGlassRow(
                    icon = Icons.Default.CloudQueue,
                    label = "Emergency Cloud Gateway",
                    isPass = networkStatus == true,
                    passText = "Online",
                    failText = if (isCheckingNetwork) "Probing..." else "SMS Only"
                )
            }
        }

        // Safe Rehearsal Drill Glass Card
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
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Safe Emergency Rehearsal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGlassPrimary
                        )
                        Text(
                            text = "Test the complete vertical slice safely",
                            fontSize = 11.sp,
                            color = TextGlassSecondary
                        )
                    }
                }

                Text(
                    text = "A test drill executes real GPS streaming and test SMS notifications without panicking contacts. All alerts are explicitly watermarked [TEST MODE - DRILL - USER IS SAFE].",
                    fontSize = 12.sp,
                    color = TextGlassSecondary,
                    lineHeight = 18.sp
                )

                GlassButton(
                    onClick = {
                        manager.triggerSosImmediate(isTestMode = true)
                        onStartDrill()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    backgroundColor = Color(0xFF222329),
                    borderColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START TEST DRILL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun DiagnosticGlassRow(
    icon: ImageVector,
    label: String,
    isPass: Boolean,
    passText: String,
    failText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPass) Color.White else TextGlassMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = TextGlassPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        GlassPill(
            text = if (isPass) passText else failText,
            tintColor = if (isPass) Color.White else Color(0xFFA1A1AA)
        )
    }
}
