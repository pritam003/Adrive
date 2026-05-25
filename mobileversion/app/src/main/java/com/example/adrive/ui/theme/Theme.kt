package com.example.adrive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AdriveBlue,
    onPrimary = Color.White,
    primaryContainer = AdriveBlueLight,
    onPrimaryContainer = Color.White,
    secondary = AdriveSubtext,
    background = AdriveSurface,
    surface = Color.White,
    onBackground = AdriveOnSurface,
    onSurface = AdriveOnSurface,
    error = AdriveError,
)

private val DarkColorScheme = darkColorScheme(
    primary = AdriveBlueLight,
    onPrimary = Color.Black,
    primaryContainer = AdriveBlueDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBBBBBB),
    background = AdriveSurfaceDark,
    surface = Color(0xFF2A2A3E),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFEF9A9A),
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

