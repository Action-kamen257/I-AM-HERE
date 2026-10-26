package com.iamhere.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.safety.FakeCallManager
import com.iamhere.emergency.ui.theme.StatusSafeGreen
import com.iamhere.emergency.ui.theme.EmergencyRed
import com.iamhere.emergency.ui.theme.CoralPrimary
import com.iamhere.emergency.ui.theme.GlassBackground
import com.iamhere.emergency.ui.theme.TextGlassPrimary
import com.iamhere.emergency.ui.theme.TextGlassSecondary

@Composable
fun FakeCallOverlay() {
    val context = LocalContext.current
    val fakeCallManager = remember { FakeCallManager.getInstance(context) }
    val isRinging by fakeCallManager.isRinging.collectAsState()
    val isConnected by fakeCallManager.isCallConnected.collectAsState()
    val durationSeconds by fakeCallManager.callDurationSeconds.collectAsState()

    if (!isRinging && !isConnected) return

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD905060A))
                .padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Space
                Spacer(modifier = Modifier.height(30.dp))

                // Caller Info Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0x25FFFFFF))
                            .border(
                                1.5.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x60FFFFFF), Color(0x15FFFFFF))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = fakeCallManager.callerName,
                            color = TextGlassPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isConnected) formattedDuration else "Incoming Call...",
                            color = if (isConnected) StatusSafeGreen else TextGlassSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // In-Call Controls if Connected
                if (isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControlGlassButton(icon = Icons.Default.Mic, label = "mute")
                        CallControlGlassButton(icon = Icons.Default.Dialpad, label = "keypad")
                        CallControlGlassButton(icon = Icons.Default.VolumeUp, label = "speaker")
                    }
                }

                // Bottom Answer / Decline Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = if (isRinging) Arrangement.SpaceEvenly else Arrangement.Center
                ) {
                    if (isRinging) {
                        // Decline Call
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRed)
                                    .border(1.5.dp, Color(0x60FFFFFF), CircleShape)
                                    .clickable { fakeCallManager.endCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Decline", color = TextGlassSecondary, fontSize = 12.sp)
                        }

                        // Answer Call
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0x40FFFFFF), CircleShape)
                                    .clickable { fakeCallManager.answerCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.Black, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Accept", color = TextGlassSecondary, fontSize = 12.sp)
                        }
                    } else {
                        // End Call
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRed)
                                    .border(1.5.dp, Color(0x60FFFFFF), CircleShape)
                                    .clickable { fakeCallManager.endCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("End Call", color = TextGlassSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0x20FFFFFF))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = TextGlassSecondary, fontSize = 11.sp)
    }
}
