package com.iamhere.emergency.ui.theme

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern Clean Application Background (Based on Emergency Call Master Reference)
 * Light #FAFAFB with soft ambient blush gradient in light mode; charcoal/burgundy in dark mode.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    showMapCanvas: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) AppBackgroundDark else AppBackgroundLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (showMapCanvas && !isDark) {
            MapBackgroundCanvas(modifier = Modifier.fillMaxSize())
        }

        // Ambient top-left soft coral glow
        Box(
            modifier = Modifier
                .size(460.dp)
                .align(Alignment.TopStart)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0x18FF6268) else Color(0x14FF6268),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        content()
    }
}

/**
 * Understated, clean vector map canvas inspired by the Reference Screenshot
 * (Shows subtle streets: "Gates Ave", "Crown Heights", "Madison St", "Park Pl")
 */
@Composable
fun MapBackgroundCanvas(modifier: Modifier = Modifier) {
    val roadColor = Color(0xFFECECF2)
    val avenueColor = Color(0xFFE2E2EA)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle diagonal avenues
        drawLine(
            color = avenueColor,
            start = Offset(-50f, h * 0.18f),
            end = Offset(w + 50f, h * 0.38f),
            strokeWidth = 9f
        )
        drawLine(
            color = avenueColor,
            start = Offset(-50f, h * 0.45f),
            end = Offset(w + 50f, h * 0.65f),
            strokeWidth = 11f
        )
        drawLine(
            color = avenueColor,
            start = Offset(-50f, h * 0.72f),
            end = Offset(w + 50f, h * 0.92f),
            strokeWidth = 9f
        )

        // Cross street grid lines
        val streetCount = 9
        for (i in 0..streetCount) {
            val x = (w / streetCount) * i
            drawLine(
                color = roadColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 4f
            )
        }

        // Horizontal residential blocks
        val blockCount = 14
        for (j in 0..blockCount) {
            val y = (h / blockCount) * j
            drawLine(
                color = roadColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 3f
            )
        }
    }
}

/**
 * Clean, Reassuring Card Surface (Light Modern Style)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    shadowElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val finalBg = backgroundColor ?: if (isDark) SurfaceDark else SurfaceWhite
    val finalBorderColor = borderColor ?: if (isDark) BorderSubtleDark else BorderSubtleLight

    Surface(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color(0x0C000000),
                spotColor = Color(0x10000000)
            )
            .clip(shape)
            .border(
                borderWidth,
                borderBrush ?: Brush.verticalGradient(listOf(finalBorderColor, finalBorderColor)),
                shape
            ),
        shape = shape,
        color = finalBg,
        contentColor = if (isDark) TextPrimaryDark else TextPrimaryLight
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Tactile Spring-Press Button
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
    content: @Composable RowScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val finalBg = backgroundColor ?: if (isDark) SurfaceDarkElevated else SurfaceLightNeutral
    val finalBorder = borderColor ?: if (isDark) BorderSubtleDark else BorderSubtleLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(finalBg)
            .border(1.dp, finalBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Modern Status Badge / Pill (Supports Safe Green, Need Help Coral, and Neutral states)
 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tintColor: Color = CoralPrimary,
    shape: Shape = CircleShape
) {
    val isDark = isSystemInDarkTheme()

    val (bg, border, fg) = when {
        tintColor == StatusSafeGreen || text.equals("Safe", ignoreCase = true) || text.equals("Delivered", ignoreCase = true) -> {
            Triple(
                if (isDark) Color(0xFF14301B) else StatusSafeGreenBg,
                if (isDark) Color(0xFF1E4626) else StatusSafeGreenBorder,
                if (isDark) Color(0xFF81C784) else StatusSafeGreen
            )
        }
        tintColor == EmergencyRed || tintColor == CoralPrimary || text.contains("Need", ignoreCase = true) || text.contains("Emergency", ignoreCase = true) || text.contains("Responding", ignoreCase = true) -> {
            Triple(
                if (isDark) Color(0xFF331619) else SoftBlush,
                if (isDark) Color(0xFF552227) else SoftBlushBorder,
                if (isDark) Color(0xFFFF8A90) else EmergencyRed
            )
        }
        tintColor == StatusWarningAmber || text.contains("Warning", ignoreCase = true) -> {
            Triple(
                if (isDark) Color(0xFF332210) else StatusWarningAmberBg,
                if (isDark) Color(0xFF553A1B) else Color(0xFFFDE68A),
                if (isDark) Color(0xFFFBBF24) else StatusWarningAmber
            )
        }
        else -> {
            Triple(
                if (isDark) Color(0xFF24242B) else Color(0xFFF1F1F5),
                if (isDark) Color(0xFF32323D) else Color(0xFFE2E2EB),
                if (isDark) TextSecondaryDark else TextSecondaryLight
            )
        }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Navigation item for the bottom navigation dock
 */
data class GlassNavItem(
    val icon: ImageVector,
    val label: String,
    val isEmergencyButton: Boolean = false
)

/**
 * Reference-Inspired Bottom Navigation Bar (White Dock with Protruding Coral Center SOS Button)
 */
@Composable
fun GlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dockBg = if (isDark) SurfaceDark else SurfaceWhite
    val dockBorder = if (isDark) BorderSubtleDark else BorderSubtleLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color(0x10000000),
                    spotColor = Color(0x18000000)
                )
                .clip(RoundedCornerShape(34.dp))
                .border(1.dp, dockBorder, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(34.dp),
            color = dockBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val isEmergency = item.isEmergencyButton
                    val interactionSource = remember { MutableInteractionSource() }

                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dockItemScale"
                    )

                    if (isEmergency) {
                        // Protruding / Hero Coral SOS Button in Center
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(CoralPrimary, CoralPressed)
                                    )
                                )
                                .shadow(8.dp, CircleShape, ambientColor = CoralGlow, spotColor = CoralGlow)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelectIndex(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        // Regular Nav Tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelectIndex(index) }
                                )
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) CoralPrimary else if (isDark) TextSecondaryDark else TextSecondaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CoralPrimary else if (isDark) TextSecondaryDark else TextSecondaryLight
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CUSTOM ANIMATED ALARM-CLOCK-TO-SOS BUTTON
 * Inspired by the AlarmClockIcon Motion Behavior:
 * - Horizontal clock shake with slight upward translation during alert / emergency.
 * - Breathing pulse on outer blush ring.
 * - Tactile spring scale on press.
 * - Immediate execution: animation NEVER delays the emergency callback!
 * - Accessible and respects reduced motion.
 */
@Composable
fun AlarmSosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActivating: Boolean = false,
    isActiveEmergency: Boolean = false,
    label: String = "SOS",
    subLabel: String = "PRESS TO ALERT"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Check reduced motion setting
    val isReducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    // Interactive Pressed Scale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sosPressScale"
    )

    // Gentle Breathing Pulse for Outer Soft Blush Ring
    val infiniteTransition = rememberInfiniteTransition(label = "sosBreathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isReducedMotion) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosBreathingScale"
    )
    val outerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = if (isReducedMotion) 0.55f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosOuterAlpha"
    )

    // Alarm-Clock Horizontal Shake & Lift Translation
    val shakeX = remember { Animatable(0f) }
    val liftY = remember { Animatable(0f) }

    LaunchedEffect(isActivating, isActiveEmergency) {
        if ((isActivating || isActiveEmergency) && !isReducedMotion) {
            // Clock-shake sequence: rapid horizontal oscillation + upward lift
            while (isActivating || isActiveEmergency) {
                // Lift slightly
                coroutineScope.launch {
                    liftY.animateTo(-5f, animationSpec = tween(60, easing = LinearEasing))
                }
                // Shake horizontal keyframes
                val keyframes = listOf(0f, -8f, 8f, -6f, 6f, -4f, 4f, 0f)
                for (x in keyframes) {
                    shakeX.animateTo(x, animationSpec = tween(40, easing = LinearEasing))
                }
                liftY.animateTo(0f, animationSpec = tween(80, easing = FastOutSlowInEasing))
                delay(600) // Brief rest between bursts
            }
        } else {
            shakeX.snapTo(0f)
            liftY.snapTo(0f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(280.dp)
            .graphicsLayer {
                translationX = shakeX.value
                translationY = liftY.value
                scaleX = pressScale
                scaleY = pressScale
            }
            .semantics {
                role = Role.Button
                contentDescription = "Emergency SOS Button. Tap to trigger distress broadcast."
            }
    ) {
        // 1. Outermost Soft Blush Ring (Matches Reference UI Screenshot)
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(breathingScale)
                .clip(CircleShape)
                .background(SoftBlush.copy(alpha = outerAlpha))
                .border(1.5.dp, SoftBlushBorder, CircleShape)
        )

        // 2. Middle Coral Accent Ring
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, CoralPrimary.copy(alpha = 0.25f), CircleShape)
        )

        // 3. Inner Solid Coral SOS Core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(185.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CoralPrimary,
                            CoralPressed
                        )
                    )
                )
                .shadow(14.dp, CircleShape, ambientColor = CoralGlow, spotColor = CoralGlow)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Alarm Bells & Handle Decorative Canvas Silhouette (Tastefully Integrated)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.width / 2f

                // Left twin bell arch (10 o'clock)
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = 210f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(cx - r + 10f, cy - r + 10f),
                    size = androidx.compose.ui.geometry.Size((r - 10f) * 2, (r - 10f) * 2),
                    style = Stroke(width = 3.5f)
                )
                // Right twin bell arch (2 o'clock)
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = 285f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(cx - r + 10f, cy - r + 10f),
                    size = androidx.compose.ui.geometry.Size((r - 10f) * 2, (r - 10f) * 2),
                    style = Stroke(width = 3.5f)
                )
            }

            // Core SOS Typography & Finger Tap Beacon (Matches Reference Screen 1)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hand Tap / Alert Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    text = subLabel,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
