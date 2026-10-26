package com.iamhere.emergency.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.R
import com.iamhere.emergency.data.model.ContactAlertStatus
import com.iamhere.emergency.data.model.DeliveryState
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SosScreen() {
    val context = LocalContext.current
    val manager = remember { EmergencyManager.getInstance() }

    val isEmergencyActive by manager.isEmergencyActive.collectAsState()
    val countdown by manager.countdown.collectAsState()
    val currentSession by manager.currentSession.collectAsState()
    val contactStatuses by manager.contactAlertStatuses.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!isEmergencyActive && countdown == null) {
            IdleSosContent(
                onTriggerCountdown = {
                    manager.startSosCountdown()
                },
                onQuickCall112 = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(intent)
                }
            )
        } else if (countdown != null) {
            ReferenceCountdownOverlay(
                secondsLeft = countdown ?: 3,
                onCancel = { manager.cancelCountdown() }
            )
        } else {
            ActiveSosDashboard(
                contactStatuses = contactStatuses,
                sessionToken = currentSession?.token,
                onOpenLiveUrl = {
                    val token = currentSession?.token
                    if (token != null) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:4000/live/$token"))
                        context.startActivity(browserIntent)
                    }
                },
                onCall112 = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(intent)
                },
                onRequestCancel = {
                    showPinDialog = true
                }
            )
        }

        // Cancel PIN Verification Dialog
        if (showPinDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SoftBlush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Deactivate Emergency",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your 4-digit safety PIN to deactivate the live distress broadcast.",
                            fontSize = 13.sp,
                            color = TextSecondaryLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 4) enteredPin = it
                                pinError = null
                            },
                            placeholder = { Text("Enter 4-digit PIN", color = TextMutedLight) },
                            singleLine = true,
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoralPrimary,
                                unfocusedBorderColor = BorderSubtleLight,
                                focusedTextColor = TextPrimaryLight,
                                unfocusedTextColor = TextPrimaryLight,
                                focusedContainerColor = SurfaceLightNeutral,
                                unfocusedContainerColor = SurfaceLightNeutral
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinError != null) {
                            Text(
                                text = pinError!!,
                                color = EmergencyRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassButton(
                                onClick = { showPinDialog = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                backgroundColor = SurfaceLightNeutral,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Dismiss", color = TextSecondaryLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            GlassButton(
                                onClick = {
                                    manager.stopSos(enteredPin) { success, msg ->
                                        if (success) {
                                            showPinDialog = false
                                            enteredPin = ""
                                        } else {
                                            pinError = msg
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(50.dp),
                                backgroundColor = CoralPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Confirm Safe", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 1: IDLE HOME SOS (Matching Master Reference UI)
 */
@Composable
private fun IdleSosContent(
    onTriggerCountdown: () -> Unit,
    onQuickCall112: () -> Unit
) {
    val manager = remember { EmergencyManager.getInstance() }
    val userPhone by manager.userPhoneFlow.collectAsState()
    val contactsList by manager.contacts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Header Row: Status Capsule & App Brand
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live Status Capsule
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .shadow(4.dp, CircleShape, ambientColor = Color(0x0C000000))
                    .border(1.dp, BorderSubtleLight, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusSafeGreen)
                )
                Text(
                    text = "LIVE GUARDIAN ARMED",
                    color = TextPrimaryLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(TextMutedLight)
                )
                Text(
                    text = "GPS ACTIVE",
                    color = StatusSafeGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Brand Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "I AM HERE Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderSubtleLight, RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "I AM HERE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "WOMEN SAFETY GUARDIAN",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = CoralPrimary
                    )
                }
            }
        }

        // 2. Center Hero: Animated Alarm-Clock-to-SOS Button (Master Reference Screen 1)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AlarmSosButton(
                onClick = onTriggerCountdown,
                label = "SOS",
                subLabel = "TAP TO ALERT"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reference Screen Location & Instruction Typography
            Text(
                text = "CROWN HEIGHTS",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap button to broadcast emergency",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your contact, app users nearby, and your organization will see your request help",
                fontSize = 12.sp,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. Quick Action Tier (112 Emergency Call & Armed Circle Status)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Direct 112 Dial Button
            GlassButton(
                onClick = onQuickCall112,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                backgroundColor = SoftBlush,
                borderColor = SoftBlushBorder,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = CoralPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Direct Emergency Dial: 112",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralPrimary,
                    letterSpacing = 0.3.sp
                )
            }

            // Circle Standby Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = SurfaceWhite,
                shadowElevation = 2.dp
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SoftBlush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${contactsList.size} Trusted Contacts Armed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = if (userPhone.isNotEmpty()) "Linked: $userPhone" else "Ready on Standby",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }
                    GlassPill(
                        text = "STANDBY",
                        tintColor = StatusSafeGreen
                    )
                }
            }
        }
    }
}

/**
 * SCREEN 2: REFERENCE COUNTDOWN OVERLAY
 * Matches Screen 2 from the master reference image:
 * - Coral emergency background
 * - Top countdown badge "3"
 * - "Emergency Calling..."
 * - "Your contact, app users nearby, and your organization will see your request help"
 * - Concentric radar circles with contact avatars in orbit!
 * - Clean white "I am safe" cancellation button
 */
@Composable
private fun ReferenceCountdownOverlay(
    secondsLeft: Int,
    onCancel: () -> Unit
) {
    val manager = remember { EmergencyManager.getInstance() }
    val contacts by manager.contacts.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        CoralPrimary,
                        CoralPressed,
                        EmergencyDarkRed
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Countdown badge & Headline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Circular White Countdown Badge (Directly from Reference Screen 2)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(12.dp, CircleShape, ambientColor = Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$secondsLeft",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = CoralPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Emergency Calling...",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your contact, app users nearby, and your organization will see your request help",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Center Section: Concentric Radar Orbit with Contact Avatars
            RadarNetworkView(
                contacts = contacts.map { it.name }
            )

            // Bottom Section: White "I am safe" Button
            GlassButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp)),
                backgroundColor = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = "I am safe",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

/**
 * Concentric Radar Circles with Center Beacon (•) and Orbiting Contact Avatars
 * (Matching Screen 2 of Reference UI)
 */
@Composable
fun RadarNetworkView(
    contacts: List<String>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulseScale"
    )

    Box(
        modifier = modifier
            .size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dashed Concentric Radar Circles Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val stroke = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )

            // Inner orbit circle
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 60f * pulseRadius,
                center = Offset(cx, cy),
                style = stroke
            )

            // Middle orbit circle
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = 100f,
                center = Offset(cx, cy),
                style = stroke
            )

            // Outer orbit circle
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = 130f,
                center = Offset(cx, cy),
                style = stroke
            )
        }

        // Center Distress Beacon (•)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(CoralPrimary)
            )
        }

        // Orbiting Contact Avatars at N, E, S, W
        val displayContacts = if (contacts.isEmpty()) {
            listOf("Dad", "Mom", "Kevin", "Estelle")
        } else {
            contacts.take(4)
        }

        val angles = listOf(-90.0, 0.0, 90.0, 180.0)

        displayContacts.forEachIndexed { i, name ->
            val angleRad = Math.toRadians(angles[i % angles.size])
            val offsetX = (100 * cos(angleRad)).dp
            val offsetY = (100 * sin(angleRad)).dp

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, SoftBlush, CircleShape)
                    .shadow(6.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "C",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralPrimary
                )
            }
        }
    }
}

/**
 * ACTIVE EMERGENCY DASHBOARD
 * Live broadcasting screen with radar telemetry, live link, 112 dial, and evidence cards
 */
@Composable
private fun ActiveSosDashboard(
    contactStatuses: List<ContactAlertStatus>,
    sessionToken: String?,
    onOpenLiveUrl: () -> Unit,
    onCall112: () -> Unit,
    onRequestCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 90.dp)
    ) {
        // Active Broadcasting Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SoftBlush,
            borderColor = SoftBlushBorder
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(CoralPrimary)
                )
                Column {
                    Text(
                        text = "EMERGENCY ACTIVE • BROADCASTING",
                        color = EmergencyRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Streaming live GPS fixes • Token: ${sessionToken?.take(8) ?: "Active"}",
                        color = TextSecondaryLight,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = onOpenLiveUrl,
                modifier = Modifier.weight(1f).height(48.dp),
                backgroundColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live Map", color = TextPrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            GlassButton(
                onClick = onCall112,
                modifier = Modifier.weight(1f).height(48.dp),
                backgroundColor = SoftBlush,
                borderColor = SoftBlushBorder,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call 112", color = CoralPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Trusted Emergency Network Status Header
        Text(
            text = "TRUSTED EMERGENCY NETWORK STATUS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondaryLight,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(contactStatuses) { contact ->
                ContactEvidenceGlassCard(contact)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // "I AM SAFE" Resolution Button
        GlassButton(
            onClick = onRequestCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            backgroundColor = CoralPrimary,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "I AM SAFE (RESOLVE SOS)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ContactEvidenceGlassCard(contact: ContactAlertStatus) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .clip(CircleShape)
                            .background(SoftBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.firstOrNull()?.uppercase() ?: "C",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralPrimary
                        )
                    }
                    Column {
                        Text(
                            text = contact.name,
                            color = TextPrimaryLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${contact.relationship} • ${contact.phone}",
                            color = TextSecondaryLight,
                            fontSize = 11.sp
                        )
                    }
                }

                // Delivery State Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (contact.deliveryState) {
                        DeliveryState.DELIVERED -> {
                            GlassPill("Delivered", tintColor = StatusSafeGreen)
                        }
                        DeliveryState.SENT -> {
                            GlassPill("Sent", tintColor = TextSecondaryLight)
                        }
                        DeliveryState.DELIVERY_UNAVAILABLE -> {
                            GlassPill("Sent (No Carrier PDU)", tintColor = TextMutedLight)
                        }
                        DeliveryState.FAILED -> {
                            GlassPill("Failed", tintColor = EmergencyRed)
                        }
                        DeliveryState.QUEUED -> {
                            GlassPill("Queued", tintColor = TextMutedLight)
                        }
                    }

                    if (contact.seen) {
                        GlassPill("Seen", tintColor = StatusSafeGreen)
                    }

                    if (contact.responding) {
                        GlassPill("Responding", tintColor = CoralPrimary)
                    }
                }
            }

            if (contact.responseNote != null) {
                Text(
                    text = "Update: \"${contact.responseNote}\"",
                    color = TextPrimaryLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
