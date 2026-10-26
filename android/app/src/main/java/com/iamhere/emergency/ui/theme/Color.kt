package com.iamhere.emergency.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// I AM HERE - REFERENCE EMERGENCY CALL THEME DESIGN TOKENS
// ============================================================================

// 1. Core Coral Red & Emergency Accents (Based on Master Reference UI)
val CoralPrimary = Color(0xFFFF6268)         // Primary SOS coral red
val CoralPressed = Color(0xFFE84855)         // Deep emergency red
val EmergencyRed = Color(0xFFE84855)         // Accessible emergency red
val EmergencyDarkRed = Color(0xFFC72E3B)     // Deep red contrast tone
val SoftBlush = Color(0xFFFFF0F1)            // Soft blush surface tint
val SoftBlushBorder = Color(0xFFFFD5D8)      // Subtle blush border
val SoftBlushDeeper = Color(0xFFFFE4E6)      // Accent blush container
val CoralGlow = Color(0x35FF6268)            // Soft radial glow

// 2. Light Theme Neutral Surfaces & Typography
val AppBackgroundLight = Color(0xFFFAFAFB)   // Clean modern light background
val SurfaceWhite = Color(0xFFFFFFFF)         // Pure white card/sheet surface
val SurfaceLightNeutral = Color(0xFFF4F4F7)  // Secondary container surface
val SurfaceLightMuted = Color(0xFFEBEBF0)    // Tertiary container surface
val BorderSubtleLight = Color(0xFFE8E8ED)    // Subtle crisp card border
val BorderMediumLight = Color(0xFFD4D4DE)    // Active/focused border

val TextPrimaryLight = Color(0xFF19191D)     // Primary text high contrast
val TextSecondaryLight = Color(0xFF777781)   // Reassuring secondary text
val TextMutedLight = Color(0xFFA1A1AB)       // Caption & helper text

// 3. Dark Theme / Night Mode Surfaces (Charcoal & Warm Burgundy)
val AppBackgroundDark = Color(0xFF121215)    // Deep charcoal background
val SurfaceDark = Color(0xFF1A1A20)          // Dark surface
val SurfaceDarkElevated = Color(0xFF24242C)  // Dark elevated card surface
val BorderSubtleDark = Color(0xFF2E2E38)     // Subtle dark border
val TextPrimaryDark = Color(0xFFFFFFFF)      // Pure white text
val TextSecondaryDark = Color(0xFFA4A4B0)    // Muted dark text
val TextMutedDark = Color(0xFF70707C)        // Secondary dark muted text

// 4. Semantic Status Accents
val StatusSafeGreen = Color(0xFF2E7D32)      // Accessible trusted green
val StatusSafeGreenBg = Color(0xFFE8F5E9)    // Soft green pill container
val StatusSafeGreenBorder = Color(0xFFC8E6C9)
val StatusNeedHelp = Color(0xFFE84855)       // Active alert / Need help coral
val StatusNeedHelpBg = Color(0xFFFFF0F1)     // Soft blush alert pill container
val StatusWarningAmber = Color(0xFFD97706)   // Accessible amber warning
val StatusWarningAmberBg = Color(0xFFFEF3C7)
val StatusInfoBlue = Color(0xFF2563EB)       // Modern royal blue info
val StatusInfoBlueBg = Color(0xFFEFF6FF)

// ============================================================================
// Backward Compatibility Mappings for Existing Screens
// ============================================================================
val NeutralWhite = Color(0xFFFFFFFF)
val NeutralZinc100 = Color(0xFFF4F4F5)
val NeutralZinc300 = Color(0xFFD4D4D8)
val NeutralZinc400 = Color(0xFFA1A1AA)
val NeutralZinc500 = Color(0xFF71717A)
val NeutralZinc700 = Color(0xFF27272A)
val NeutralZinc800 = Color(0xFF1E1F24)
val NeutralZinc900 = Color(0xFF141518)
val NeutralZinc950 = Color(0xFF0D0E11)

val AccentSuccess = StatusSafeGreen
val AccentWarning = StatusWarningAmber
val AccentInfo = StatusInfoBlue
val AccentPurple = Color(0xFF7C3AED)
val AccentTeal = Color(0xFF0D9488)

val EmergencyCardBg = SurfaceWhite
val EmergencyCardBorder = BorderSubtleLight
val EmergencyBg = AppBackgroundLight
val EmergencySurface = SoftBlush

val GlassSurface = Color(0xFFFFFFFF)
val GlassSurfaceHigh = Color(0xFFF8F8FA)
val GlassSurfaceUltra = Color(0xFFF0F0F4)
val GlassSurfaceDark = SurfaceDark
val GlassSurfaceCard = SurfaceWhite
val GlassBorder = BorderSubtleLight
val GlassBorderDim = Color(0x18000000)
val GlassBorderLight = BorderSubtleLight

val TextPrimary = TextPrimaryLight
val TextSecondary = TextSecondaryLight
val TextMuted = TextMutedLight
val TextGlassPrimary = TextPrimaryLight
val TextGlassSecondary = TextSecondaryLight
val TextGlassMuted = TextMutedLight

