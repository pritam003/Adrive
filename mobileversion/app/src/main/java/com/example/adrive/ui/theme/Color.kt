package com.example.adrive.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Primary palette (Indigo → Violet → Cyan accent) ─────────────────────────
val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Violet500 = Color(0xFF8B5CF6)
val Violet400 = Color(0xFFA78BFA)
val Cyan400  = Color(0xFF22D3EE)
val Pink400  = Color(0xFFF472B6)

// ── Surface tones ────────────────────────────────────────────────────────────
val SurfaceLight   = Color(0xFFFAFAFE)
val SurfaceCard    = Color(0xFFFFFFFF)
val SurfaceMuted   = Color(0xFFF1F2F8)
val OnSurfaceDark  = Color(0xFF1B1B2A)
val Subtext        = Color(0xFF6B6F84)

val SurfaceDarkBg  = Color(0xFF0F1020)
val SurfaceDarkCard = Color(0xFF1B1D33)

// ── Status ───────────────────────────────────────────────────────────────────
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// ── Backwards-compat alias (used widely as AdriveBlue) ───────────────────────
val AdriveBlue = Indigo600
val AdriveBlueLight = Violet400
val AdriveBlueDark = Color(0xFF3730A3)
val AdriveSurface = SurfaceLight
val AdriveSurfaceDark = SurfaceDarkBg
val AdriveOnSurface = OnSurfaceDark
val AdriveSubtext = Subtext
val AdriveError = ErrorRed
val AdriveSuccess = Success
val AdriveWarning = Warning

// ── Reusable gradients ───────────────────────────────────────────────────────
val BrandGradient = Brush.linearGradient(
    colors = listOf(Indigo600, Violet500, Pink400)
)
val SoftBrandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEEF2FF), Color(0xFFFAE8FF))
)
val FolderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFE4E6))
)
val LoginBgGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFEEF2FF), Color(0xFFFAE8FF), Color(0xFFFDF2F8))
)
val LoginBgGradientDark = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F1020), Color(0xFF1B1D33), Color(0xFF312E81))
)
