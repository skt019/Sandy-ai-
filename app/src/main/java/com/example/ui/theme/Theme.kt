package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68),
    onPrimaryContainer = Color(0xFFBEE9FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF1E2678),
    secondaryContainer = Color(0xFF353D90),
    onSecondaryContainer = Color(0xFFE0E0FF),
    tertiary = NeonPurple,
    onTertiary = Color(0xFF381E72),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = CoralRose
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8FF),
    onPrimaryContainer = Color(0xFF001E2B),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF0B125B),
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = CoralRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek cyber dark theme for local AI experience
    dynamicColor: Boolean = false, // Keep consistent cyber theme
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
