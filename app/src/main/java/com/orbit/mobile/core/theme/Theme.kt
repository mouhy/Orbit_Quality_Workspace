package com.orbit.mobile.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Dark scheme
private val DarkScheme = darkColorScheme(
    primary = OrbitPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF152E5C),
    onPrimaryContainer = Color(0xFFD7E4FF),
    secondary = OrbitSky,
    onSecondary = Color.White,
    tertiary = OrbitPurple,
    onTertiary = Color.White,
    error = OrbitDanger,
    onError = Color.White,
    background = Color(0xFF0A0C14),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111420),
    onSurface = Color(0xFFF0F4F9),
    surfaceVariant = Color(0xFF161924),
    onSurfaceVariant = Color(0x9EFFFFFF),
    outline = Color(0x12FFFFFF),
    outlineVariant = Color(0x0DFFFFFF),
    surfaceContainerLowest = Color(0xFF0A0C14),
    surfaceContainerLow = Color(0xFF111420),
    surfaceContainer = Color(0xFF161924),
    surfaceContainerHigh = Color(0xFF1C2030),
    surfaceContainerHighest = Color(0xFF1D2235)
)

// Light scheme
private val LightScheme = lightColorScheme(
    primary = OrbitPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE7FF),
    onPrimaryContainer = Color(0xFF0B2E6B),
    secondary = OrbitSky,
    onSecondary = Color.White,
    tertiary = OrbitPurple,
    onTertiary = Color.White,
    error = OrbitDanger,
    onError = Color.White,
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x14000000),
    outlineVariant = Color(0x0D000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFEDF2F7),
    surfaceContainerHighest = Color(0xFFE2E8F0)
)

// App theme
@Composable
fun OrbitTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extended = if (darkTheme) DarkOrbitColors else LightOrbitColors
    CompositionLocalProvider(LocalOrbitColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = OrbitTypography,
            shapes = OrbitShapes,
            content = content
        )
    }
}

// Token accessor
object OrbitTheme {
    val colors: OrbitExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitColors.current
}
