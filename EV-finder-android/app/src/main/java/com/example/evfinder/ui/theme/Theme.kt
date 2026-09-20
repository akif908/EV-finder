package com.example.evfinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Voltage Mobility design system — values taken from the UI mockups
 * (design/voltage_mobility). Dark tonal surfaces + electric green accent.
 */
object EvColors {
    // surfaces (tonal elevation, not shadows)
    val Background        = Color(0xFF131313)
    val SurfaceLowest     = Color(0xFF0E0E0E)
    val SurfaceLow        = Color(0xFF1C1B1B)
    val Surface           = Color(0xFF201F1F)
    val SurfaceHigh       = Color(0xFF2A2A2A)
    val SurfaceHighest    = Color(0xFF353534)
    val SurfaceBright     = Color(0xFF393939)

    // text
    val OnBackground      = Color(0xFFE5E2E1)
    val OnSurface         = Color(0xFFE5E2E1)
    val OnSurfaceVar      = Color(0xFFBCCBB9)
    val Outline           = Color(0xFF869585)
    val OutlineVariant    = Color(0xFF3D4A3D)
    /** Alias kept for the older screens that used this name. */
    val SurfaceBorder     = Color(0xFF3D4A3D)

    // accent
    val Primary           = Color(0xFF4BE277)
    val OnPrimary         = Color(0xFF003915)
    val PrimaryContainer  = Color(0xFF22C55E)
    val OnPrimaryContainer= Color(0xFF004B1E)
    val PrimaryFixed      = Color(0xFF6BFF8F)
    val PrimaryFixedDim   = Color(0xFF4AE176)
    val PrimaryDim        = Color(0xFF1A3A27)

    val Secondary         = Color(0xFF4EDEA3)
    val Tertiary          = Color(0xFFAFC7FF)
    val Error             = Color(0xFFFFB4AB)
    val ErrorContainer    = Color(0xFF93000A)
    val Warning           = Color(0xFFFFBB33)

    // form fields (auth/settings screens use these literals)
    val InputBackground   = Color(0xFF1A1A1A)
    val InputBorder       = Color(0xFF2E2E2E)
}

private val EvColorScheme = darkColorScheme(
    primary              = EvColors.Primary,
    onPrimary            = EvColors.OnPrimary,
    primaryContainer     = EvColors.PrimaryContainer,
    onPrimaryContainer   = EvColors.OnPrimaryContainer,
    secondary            = EvColors.Secondary,
    onSecondary          = EvColors.OnPrimary,
    tertiary             = EvColors.Tertiary,
    background           = EvColors.Background,
    onBackground         = EvColors.OnBackground,
    surface              = EvColors.Surface,
    onSurface            = EvColors.OnSurface,
    surfaceVariant       = EvColors.SurfaceHighest,
    onSurfaceVariant     = EvColors.OnSurfaceVar,
    outline              = EvColors.Outline,
    outlineVariant       = EvColors.OutlineVariant,
    error                = EvColors.Error,
    errorContainer       = EvColors.ErrorContainer
)

// Plus Jakarta Sans is the mockup font; without shipping the font files we use
// the platform sans with the same weights/sizes, which reads nearly identically.
private val EvFont = FontFamily.SansSerif

private val EvTypography = Typography(
    displayLarge   = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp),
    headlineLarge  = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineSmall  = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium    = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 28.sp),
    titleSmall     = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge      = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium     = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall      = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall     = TextStyle(fontFamily = EvFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
)

@Composable
fun EVFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EvColorScheme,
        typography = EvTypography,
        content = content
    )
}
