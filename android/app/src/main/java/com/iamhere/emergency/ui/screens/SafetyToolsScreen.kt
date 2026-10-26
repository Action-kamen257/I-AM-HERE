package com.iamhere.emergency.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.iamhere.emergency.safety.FakeCallManager
import com.iamhere.emergency.safety.JourneyManager
import com.iamhere.emergency.safety.SafePoiRepository
import com.iamhere.emergency.ui.theme.*

@Composable
fun SafetyToolsScreen() {
    val context = LocalContext.current
    val journeyManager = remember { JourneyManager.getInstance(context) }
    val fakeCallManager = remember { FakeCallManager.getInstance(context) }

    val activeJourney by journeyManager.activeJourney.collectAsState()
    val isCheckInPending by journeyManager.isCheckInPending.collectAsState()
    val checkInGraceSec by journeyManager.checkInGraceSeconds.collectAsState()

    val isCallPending by fakeCallManager.isCallPending.collectAsState()
    val pendingCountdown by fakeCallManager.pendingCountdown.collectAsState()

    var showStartJourneyDialog by remember { mutableStateOf(false) }
    var journeyDestination by remember { mutableStateOf("Walking Home") }
    var journeyDurationMinutes by remember { mutableIntStateOf(20) }
    var checkInIntervalMinutes by remember { mutableIntStateOf(5) }

    var selectedCaller by remember { mutableStateOf("Dad") }
    var selectedDelaySec by remember { mutableIntStateOf(5) }

    val safePois = remember { SafePoiRepository.getNearbySafePoints() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Safety Companion",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Proactive journey tracking, safe points, and discreet deterrents",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )
            }
        }

        // Section 1: Walk With Me Journey Companion
        item {
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
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = CoralPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Walk With Me",
                                color = TextPrimaryLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (activeJourney != null) {
                            GlassPill("TRACKING ACTIVE", tintColor = StatusSafeGreen)
                        } else {
                            GlassPill("STANDBY", tintColor = TextSecondaryLight)
                        }
                    }

                    if (activeJourney == null) {
                        Text(
                            text = "Traveling or walking home alone? Start a journey timer. If you miss a safety check-in, the app alerts your primary circle automatically.",
                            color = TextSecondaryLight,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )

                        GlassButton(
                            onClick = { showStartJourneyDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            backgroundColor = CoralPrimary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Start Walk With Me",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val session = activeJourney!!
                        Text(
                            text = "Destination: ${session.destination}",
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Check-in frequency: Every ${session.checkInIntervalMinutes} mins",
                            color = TextSecondaryLight,
                            fontSize = 12.sp
                        )

                        if (isCheckInPending) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = SoftBlush,
                                borderColor = SoftBlushBorder
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("CHECK-IN REQUIRED", color = EmergencyRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Grace countdown: ${checkInGraceSec ?: 60}s before auto-SOS", color = TextPrimaryLight, fontSize = 12.sp)

                                    GlassButton(
                                        onClick = { journeyManager.checkInSafe() },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        backgroundColor = CoralPrimary,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("I AM SAFE (Check In)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            GlassButton(
                                onClick = { journeyManager.checkInSafe() },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                backgroundColor = SurfaceLightNeutral,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Check In Early (I'm Safe)", color = TextPrimaryLight, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        GlassButton(
                            onClick = {
                                journeyManager.endJourney()
                                Toast.makeText(context, "Journey ended safely", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            backgroundColor = SurfaceLightNeutral,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Arrived Safely • End Journey", color = TextSecondaryLight, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Discreet Fake Call Generator
        item {
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
                                imageVector = Icons.Default.PhoneCallback,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Discreet Fake Call",
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Simulates a realistic incoming call with ringtone and audio cues to deter harassment or gracefully exit uncomfortable encounters.",
                        color = TextSecondaryLight,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Text("SELECT CALLER", color = TextSecondaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Dad", "Boss", "Police", "Mom").forEach { name ->
                            val isSelected = selectedCaller == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                    .border(
                                        1.dp,
                                        if (isSelected) CoralPrimary else BorderSubtleLight,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedCaller = name },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextPrimaryLight,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Text("TRIGGER DELAY", color = TextSecondaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5 to "5s", 15 to "15s", 30 to "30s", 60 to "1m").forEach { (sec, label) ->
                            val isSelected = selectedDelaySec == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                    .border(
                                        1.dp,
                                        if (isSelected) CoralPrimary else BorderSubtleLight,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedDelaySec = sec },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextPrimaryLight,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (isCallPending) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftBlush)
                                .border(1.dp, SoftBlushBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Incoming call in ${pendingCountdown ?: 0}s...",
                                color = CoralPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = { fakeCallManager.cancelPending() }) {
                                Text("Cancel", color = EmergencyRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        GlassButton(
                            onClick = {
                                fakeCallManager.scheduleFakeCall(selectedCaller, selectedDelaySec)
                                Toast.makeText(context, "Fake call scheduled in ${selectedDelaySec}s", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            backgroundColor = CoralPrimary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Fake Call", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Section 3: Safe Points of Interest (POIs)
        item {
            Text(
                text = "NEARBY SAFE HAVENS & EMERGENCY DESKS",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryLight,
                letterSpacing = 0.8.sp
            )
        }

        items(safePois) { poi ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = SurfaceWhite,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (poi.category) {
                                    "POLICE" -> Icons.Default.LocalPolice
                                    "HOSPITAL" -> Icons.Default.LocalHospital
                                    "WOMEN_HELPLINE" -> Icons.Default.SupportAgent
                                    else -> Icons.Default.Shield
                                },
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(poi.name, color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(poi.address, color = TextSecondaryLight, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        GlassPill("${poi.approxDistanceKm} km • 24/7 Active", tintColor = StatusSafeGreen)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { SafePoiRepository.openNavigation(context, poi) }) {
                            Icon(Icons.Default.Directions, contentDescription = "Navigate", tint = CoralPrimary)
                        }
                        IconButton(onClick = { SafePoiRepository.dialPhone(context, poi.phone) }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = StatusSafeGreen)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Start Journey Dialog
    if (showStartJourneyDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { showStartJourneyDialog = false }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Start Walk With Me", color = TextPrimaryLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    OutlinedTextField(
                        value = journeyDestination,
                        onValueChange = { journeyDestination = it },
                        placeholder = { Text("Destination (e.g. Walking Home)", color = TextMutedLight) },
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

                    Text("TOTAL DURATION", fontSize = 11.sp, color = TextSecondaryLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            val isSelected = journeyDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                    .border(1.dp, if (isSelected) CoralPrimary else BorderSubtleLight, RoundedCornerShape(10.dp))
                                    .clickable { journeyDurationMinutes = mins }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${mins}m", color = if (isSelected) Color.White else TextPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("CHECK-IN INTERVAL", fontSize = 11.sp, color = TextSecondaryLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15).forEach { mins ->
                            val isSelected = checkInIntervalMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                    .border(1.dp, if (isSelected) CoralPrimary else BorderSubtleLight, RoundedCornerShape(10.dp))
                                    .clickable { checkInIntervalMinutes = mins }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${mins}m", color = if (isSelected) Color.White else TextPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            onClick = { showStartJourneyDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            backgroundColor = SurfaceLightNeutral,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel", color = TextSecondaryLight, fontSize = 13.sp)
                        }

                        GlassButton(
                            onClick = {
                                journeyManager.startJourney(journeyDestination, journeyDurationMinutes, checkInIntervalMinutes)
                                showStartJourneyDialog = false
                            },
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            backgroundColor = CoralPrimary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Start Tracking", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
