package com.eventradar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF0F1420)
val Panel = Color(0xFF182031)
val PanelHigh = Color(0xFF222C42)
val Flare = Color(0xFFFF5A36)     // primary accent — racing orange
val Signal = Color(0xFF3DD6C6)    // secondary accent
val Muted = Color(0xFF8C97AD)

/** Stable colour per interest so tags and calendar dots stay consistent. */
private val tagPalette = listOf(
    Color(0xFFFF5A36), Color(0xFF3DD6C6), Color(0xFFFFC145), Color(0xFF8E7CFF),
    Color(0xFF5BC0FF), Color(0xFFFF7AB6), Color(0xFF9BE15D), Color(0xFFFF9F43),
)
fun tagColor(name: String): Color = tagPalette[(name.lowercase().hashCode() and 0x7fffffff) % tagPalette.size]

private val scheme = darkColorScheme(
    primary = Flare,
    onPrimary = Color.White,
    secondary = Signal,
    onSecondary = Ink,
    background = Ink,
    onBackground = Color(0xFFEDF0F7),
    surface = Ink,
    onSurface = Color(0xFFEDF0F7),
    surfaceVariant = Panel,
    onSurfaceVariant = Muted,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelHigh,
    surfaceContainerLow = Panel,
    secondaryContainer = PanelHigh,
    onSecondaryContainer = Color.White,
    outline = Color(0xFF3A4560),
    outlineVariant = Color(0xFF2A3348),
)

private val type = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    )
}

@Composable
fun EventRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
