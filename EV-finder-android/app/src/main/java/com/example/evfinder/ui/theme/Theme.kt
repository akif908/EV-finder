package com.example.evfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors mirror the UI design system (Voltage Mobility: dark surface + green primary)
private val EvColorScheme = darkColorScheme(
    primary = Color(0xFF4BE277),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF22C55E),
    onPrimaryContainer = Color(0xFF004B1E),
    secondary = Color(0xFF4EDEA3),
    background = Color(0xFF131313),
    onBackground = Color(0xFFE5E2E1),
    surface = Color(0xFF201F1F),
    onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF353534),
    onSurfaceVariant = Color(0xFFBCCBB9),
    outline = Color(0xFF869585),
    error = Color(0xFFFFB4AB)
)

@Composable
fun EVFinderTheme(content: @Composable () -> Unit) {
    // App is designed dark-first; keep the same palette regardless of system setting
    MaterialTheme(
        colorScheme = EvColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
