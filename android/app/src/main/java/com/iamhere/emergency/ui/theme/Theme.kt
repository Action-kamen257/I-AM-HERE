package com.iamhere.emergency.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftBlush,
    onPrimaryContainer = EmergencyRed,
    secondary = StatusInfoBlue,
    onSecondary = Color.White,
    tertiary = StatusSafeGreen,
    onTertiary = Color.White,
    background = AppBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLightNeutral,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtleLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C1619),
    onPrimaryContainer = SoftBlush,
    secondary = StatusInfoBlue,
    onSecondary = Color.White,
    tertiary = StatusSafeGreen,
    onTertiary = Color.White,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark
)

@Composable
fun IAmHereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = if (darkTheme) AppBackgroundDark else AppBackgroundLight
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

