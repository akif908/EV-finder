package com.example.evfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand Colours (Voltage Mobility / dark surface + electric green) ─────────
object EvColors {
    val Background     = Color(0xFF0D0D0D)   // near-black canvas
    val Surface        = Color(0xFF1A1A1A)   // card surface
    val SurfaceHigh    = Color(0xFF242424)   // elevated card
    val SurfaceBorder  = Color(0xFF2E2E2E)   // subtle border / divider

    val Primary        = Color(0xFF39E075)   // electric green CTA
    val PrimaryDark    = Color(0xFF27A854)   // darker shade for pressed
    val PrimaryDim     = Color(0xFF1A3A27)   // faint green container

    val OnPrimary      = Color(0xFF001E0D)
    val OnBackground   = Color(0xFFEEEEEE)
    val OnSurface      = Color(0xFFDDDDDD)
    val OnSurfaceVar   = Color(0xFF888888)

    val Accent         = Color(0xFF4AE886)   // lighter green for chips/badges
    val Error          = Color(0xFFFF5C5C)
    val Warning        = Color(0xFFFFBB33)
    val Success        = Color(0xFF39E075)
}

private val EvColorScheme = darkColorScheme(
    primary              = EvColors.Primary,
    onPrimary            = EvColors.OnPrimary,
    primaryContainer     = EvColors.PrimaryDim,
    onPrimaryContainer   = EvColors.Accent,
    secondary            = EvColors.Accent,
    onSecondary          = EvColors.OnPrimary,
    secondaryContainer   = EvColors.PrimaryDim,
    onSecondaryContainer = EvColors.Accent,
    tertiary             = EvColors.Warning,
    background           = EvColors.Background,
    onBackground         = EvColors.OnBackground,
    surface              = EvColors.Surface,
    onSurface            = EvColors.OnSurface,
    surfaceVariant       = EvColors.SurfaceHigh,
    onSurfaceVariant     = EvColors.OnSurfaceVar,
    outline              = EvColors.SurfaceBorder,
    error                = EvColors.Error
)

// ─── Typography (system fonts – no external dependency needed) ────────────────
private val EvTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 36.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 30.sp, letterSpacing = (-0.3).sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.Bold,   fontSize = 26.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, color = EvColors.OnSurfaceVar),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 0.4.sp)
)

@Composable
fun EVFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EvColorScheme,
        typography  = EvTypography,
        content     = content
    )
}
