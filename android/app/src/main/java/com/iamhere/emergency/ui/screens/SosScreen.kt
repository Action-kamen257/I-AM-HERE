package com.iamhere.emergency.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.R
import com.iamhere.emergency.data.model.ContactAlertStatus
import com.iamhere.emergency.data.model.DeliveryState
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*

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
            CountdownOverlay(
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

        // Cancel PIN Glass Dialog
        if (showPinDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD905060A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = Color(0xF5141728)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassPill(
                            text = "SAFETY VERIFICATION",
                            tintColor = AccentInfo,
                            icon = Icons.Default.Lock
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Deactivate Emergency",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGlassPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your 4-digit safety PIN to deactivate the live distress broadcast.",
                            fontSize = 13.sp,
                            color = TextGlassSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 4) enteredPin = it
                                pinError = null
                            },
                            placeholder = { Text("Enter 4-digit PIN", color = TextGlassMuted) },
                            singleLine = true,
                            isError = pinError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmergencyRed,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextGlassPrimary,
                                unfocusedTextColor = TextGlassPrimary,
                                focusedContainerColor = Color(0x18FFFFFF),
                                unfocusedContainerColor = Color(0x10FFFFFF)
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
                                backgroundColor = Color(0x15FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Dismiss", color = TextGlassSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                                backgroundColor = EmergencyRed.copy(alpha = 0.35f),
                                borderColor = EmergencyRed,
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

@Composable
private fun IdleSosContent(
    onTriggerCountdown: () -> Unit,
    onQuickCall112: () -> Unit
) {
    val manager = remember { EmergencyManager.getInstance() }
    val userPhone by manager.userPhoneFlow.collectAsState()
    val userName by manager.userNameFlow.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Header & Dynamic Status Capsule
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status Capsule
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xF0121316))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x35FFFFFF), Color(0x0CFFFFFF))
                        ),
                        CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "LIVE GUARDIAN ARMED",
                    color = TextGlassPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(TextGlassMuted)
                )
                Text(
                    text = "GPS ACTIVE",
                    color = TextGlassSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Brand Emblem
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "I AM HERE Logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x45FFFFFF), RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "I AM HERE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = TextGlassPrimary
                    )
                    Text(
                        text = "WOMEN SAFETY GUARDIAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = EmergencyRed
                    )
                    if (userPhone.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PROTECTED: $userPhone",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = TextGlassSecondary
                        )
                    }
                }
            }
        }

        // 2. Center Hero: The Glassmorphic Emergency SOS Orb
        val orbInteractionSource = remember { MutableInteractionSource() }
        val isOrbPressed by orbInteractionSource.collectIsPressedAsState()

        val orbTouchScale by animateFloatAsState(
            targetValue = if (isOrbPressed) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "orbTouchScale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(290.dp)
                .graphicsLayer {
                    scaleX = orbTouchScale
                    scaleY = orbTouchScale
                }
        ) {
            // Ambient Fluid Chromatic Aura
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EmergencyRed.copy(alpha = auraAlpha),
                                EmergencyDarkRed.copy(alpha = auraAlpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer Glass Ultrasonic Ripple Ring
            Box(
                modifier = Modifier
                    .size(244.dp)
                    .clip(CircleShape)
                    .background(Color(0x10FFFFFF))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x40FFFFFF), Color(0x10FFFFFF), Color(0x05FFFFFF))
                        ),
                        CircleShape
                    )
            )

            // Core Glass Sphere
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(196.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EmergencyRed,
                                Color(0xFFC2185B),
                                Color(0xFF88001F)
                            )
                        )
                    )
                    .border(
                        2.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x80FFFFFF),
                                Color(0x25FFFFFF),
                                Color(0x05FFFFFF)
                            )
                        ),
                        CircleShape
                    )
                    .clickable(
                        interactionSource = orbInteractionSource,
                        indication = null,
                        onClick = onTriggerCountdown
                    )
            ) {
                // Top Specular Reflection Crescent (Simulates 3D curved glass refraction)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x45FFFFFF),
                                    Color(0x15FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Orb Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PRESS TO ALERT",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 3. Quick Action Tier
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Direct Emergency Dial: 112 Glass Button
            GlassButton(
                onClick = onQuickCall112,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                backgroundColor = Color(0xFF222329),
                borderColor = Color(0x35FFFFFF),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Direct Emergency Dial: 112",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
            }

            // Circle Standby Peek Glass Card
            val contactsList by manager.contacts.collectAsState()
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = Color(0xF0121316)
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${contactsList.size} Trusted Contacts Armed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextGlassPrimary
                        )
                    }
                    GlassPill(
                        text = "STANDBY",
                        tintColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownOverlay(
    secondsLeft: Int,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE605060A))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = Color(0xF0121526)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPill(
                    text = "DISTRESS TRIGGER ARMED",
                    tintColor = EmergencyRed,
                    icon = Icons.Default.Warning
                )

                Text(
                    text = "$secondsLeft",
                    color = EmergencyRed,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )

                Text(
                    text = "Dispatches emergency multi-part SMS, starts live GPS streaming, and alerts your primary circle in $secondsLeft seconds.",
                    color = TextGlassSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    backgroundColor = Color(0x30FFFFFF),
                    borderColor = Color(0x50FFFFFF),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CANCEL ALERT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

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
        // Active Broadcasting Glass Banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = EmergencyDarkRed.copy(alpha = 0.7f),
            borderColor = EmergencyRed
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Column {
                    Text(
                        text = "EMERGENCY ACTIVE • BROADCASTING",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Streaming live GPS fixes and battery telemetry",
                        color = Color.White.copy(alpha = 0.8f),
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
                backgroundColor = Color(0x20FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = TextGlassPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live Map", color = TextGlassPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            GlassButton(
                onClick = onCall112,
                modifier = Modifier.weight(1f).height(48.dp),
                backgroundColor = Color(0xFF222329),
                borderColor = Color(0x35FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call 112", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Evidence-Based Contact Tracking Matrix
        Text(
            text = "TRUSTED EMERGENCY NETWORK STATUS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextGlassMuted,
            letterSpacing = 1.sp,
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

        // "I AM SAFE" Glass Resolution Button
        GlassButton(
            onClick = onRequestCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            backgroundColor = Color(0xFF222329),
            borderColor = Color.White,
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
        backgroundColor = Color(0x99121526)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = contact.name,
                        color = TextGlassPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contact.relationship} • ${contact.phone}",
                        color = TextGlassSecondary,
                        fontSize = 11.sp
                    )
                }

                // Delivery State Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (contact.deliveryState) {
                        DeliveryState.DELIVERED -> {
                            GlassPill("Delivered", tintColor = Color.White)
                        }
                        DeliveryState.SENT -> {
                            GlassPill("Sent", tintColor = Color(0xFFD4D4D8))
                        }
                        DeliveryState.DELIVERY_UNAVAILABLE -> {
                            GlassPill("Sent (No Carrier PDU)", tintColor = Color(0xFFA1A1AA))
                        }
                        DeliveryState.FAILED -> {
                            GlassPill("Failed", tintColor = EmergencyRed)
                        }
                        DeliveryState.QUEUED -> {
                            GlassPill("Queued", tintColor = TextGlassMuted)
                        }
                    }

                    if (contact.seen) {
                        GlassPill("Seen", tintColor = Color.White)
                    }

                    if (contact.responding) {
                        GlassPill("Responding", tintColor = EmergencyRed)
                    }
                }
            }

            if (contact.responseNote != null) {
                Text(
                    text = "Update: \"${contact.responseNote}\"",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
