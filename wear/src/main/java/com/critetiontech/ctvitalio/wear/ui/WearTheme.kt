package com.critetiontech.ctvitalio.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Vitalio brand blue (#1564ED, same as the phone app) tuned for a black OLED watch face. */
private val VitalioWearColors = ColorScheme(
    primary = Color(0xFF8DB4FF),
    primaryDim = Color(0xFF6E9BF5),
    primaryContainer = Color(0xFF1564ED),
    onPrimary = Color(0xFF002A6B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFB7C6E6),
    secondaryContainer = Color(0xFF26324A),
    onSecondary = Color(0xFF1B2638),
    onSecondaryContainer = Color(0xFFDCE5F8),
    tertiary = Color(0xFF6FE3B4),
    tertiaryContainer = Color(0xFF0F4D38),
    onTertiary = Color(0xFF003824),
    onTertiaryContainer = Color(0xFFA8F5D2),
    surfaceContainerLow = Color(0xFF111318),
    surfaceContainer = Color(0xFF1A1D24),
    surfaceContainerHigh = Color(0xFF252932),
    onSurface = Color(0xFFF1F3F8),
    onSurfaceVariant = Color(0xFFADB4C2),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF3A3F4A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF1F3F8),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1D1A),
    onError = Color(0xFF4A0C08),
    onErrorContainer = Color(0xFFFFDAD5),
)

/** Accent for "needs attention but not critical" (vital warnings). */
internal val WarningColor = Color(0xFFFFC266)
internal val WarningContainer = Color(0xFF4A3510)

@Composable
fun VitalioWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VitalioWearColors, content = content)
}
