package com.iamhere.emergency.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * OrbiLens Ambient Obsidian Background (Clean, minimal, zero flashy colors)
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0D))
    ) {
        // Subtle pure white radial light at top-left (OrbiLens signature)
        Box(
            modifier = Modifier
                .size(520.dp)
                .align(Alignment.TopStart)
                .offset(x = (-160).dp, y = (-160).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x0CFFFFFF),
                            Color(0x03FFFFFF),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Subtle ambient luminance at bottom-right
        Box(
            modifier = Modifier
                .size(460.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x08FFFFFF),
                            Color(0x02FFFFFF),
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
 * Frosted Glass Card with refined specular light border (OrbiLens style)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassSurfaceCard,
    borderWidth: Dp = 1.dp,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalBorderBrush = borderBrush ?: if (borderColor != null) {
        Brush.verticalGradient(
            listOf(
                borderColor,
                borderColor.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x35FFFFFF),
                Color(0x10FFFFFF),
                Color(0x04FFFFFF)
            )
        )
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, finalBorderBrush, shape),
        shape = shape,
        color = backgroundColor,
        contentColor = TextGlassPrimary
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Tactile Spring-Press Glass Button
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = GlassSurfaceHigh,
    borderColor: Color = GlassBorder,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
    content: @Composable RowScope.() -> Unit
) {
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

    val specularBrush = Brush.verticalGradient(
        listOf(
            borderColor.copy(alpha = if (isPressed) 0.9f else 0.5f),
            Color(0x0AFFFFFF)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, specularBrush, shape)
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
 * Minimalist Status Pill (monochrome with high contrast or Red for emergency)
 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tintColor: Color = Color.White,
    shape: Shape = CircleShape
) {
    val isEmergency = (tintColor == EmergencyRed || tintColor == EmergencyDarkRed)
    val bgAlpha = if (isEmergency) 0.20f else 0.10f
    val borderAlpha = if (isEmergency) 0.55f else 0.25f

    Row(
        modifier = modifier
            .clip(shape)
            .background(tintColor.copy(alpha = bgAlpha))
            .border(0.75.dp, tintColor.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 9.dp, vertical = 4.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = text,
            color = tintColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Navigation item for the OrbiLens Capsule Dock
 */
data class GlassNavItem(
    val icon: ImageVector,
    val label: String,
    val isEmergencyButton: Boolean = false
)

/**
 * OrbiLens Floating Capsule Dock (Positioned at bottom with active Light White Bar)
 */
@Composable
fun GlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(32.dp)
    val dockBorderBrush = Brush.verticalGradient(
        listOf(
            Color(0x35FFFFFF),
            Color(0x12FFFFFF),
            Color(0x06FFFFFF)
        )
    )

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
                .height(60.dp)
                .clip(dockShape)
                .border(1.dp, dockBorderBrush, dockShape),
            shape = dockShape,
            color = Color(0xF5111215),
            shadowElevation = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val isEmergency = item.isEmergencyButton
                    val interactionSource = remember { MutableInteractionSource() }

                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dockItemScale"
                    )

                    val squircleShape = RoundedCornerShape(16.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                            .clip(squircleShape)
                            .background(
                                when {
                                    isEmergency && isSelected -> EmergencyRed
                                    isEmergency -> EmergencyRed.copy(alpha = 0.22f)
                                    isSelected -> Color(0xFF25262C)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isSelected || isEmergency) 1.dp else 0.dp,
                                color = when {
                                    isEmergency && isSelected -> Color.White.copy(alpha = 0.8f)
                                    isEmergency -> EmergencyRed.copy(alpha = 0.65f)
                                    isSelected -> Color(0x35FFFFFF)
                                    else -> Color.Transparent
                                },
                                shape = squircleShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelectIndex(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Light White Bar indicator for active tab (OrbiLens feature)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(1.25.dp))
                                        .background(if (isEmergency) Color.White else Color.White)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            } else {
                                Spacer(modifier = Modifier.height(6.5.dp))
                            }

                            // Clean Icon
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = when {
                                    isEmergency && isSelected -> Color.White
                                    isEmergency -> EmergencyRed
                                    isSelected -> Color.White
                                    else -> Color(0xFF71717A)
                                },
                                modifier = Modifier.size(23.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

