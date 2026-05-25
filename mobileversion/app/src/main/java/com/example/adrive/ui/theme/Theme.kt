package com.example.adrive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Indigo600,
    secondary = Violet500,
    onSecondary = Color.White,
    tertiary = Pink400,
    background = SurfaceLight,
    surface = SurfaceCard,
    surfaceVariant = SurfaceMuted,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = Subtext,
    error = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = Violet400,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFEEF2FF),
    secondary = Cyan400,
    background = SurfaceDarkBg,
    surface = SurfaceDarkCard,
    surfaceVariant = Color(0xFF2A2D45),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8BBCC),
    error = Color(0xFFFCA5A5),
)

@Composable
fun AdriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = AdriveTypography,
        content = content
    )
}
