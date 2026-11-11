/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HiResGold = Color(0xFFFFB300)
val DirectUsbCyan = Color(0xFF00E5FF)
val BitPerfectGreen = Color(0xFF00E676)
val AmoledSurfaceCard = Color(0xFF12161F)
val AmoledElevatedCard = Color(0xFF1A202C)

private val AmoledAudiophileColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),          // Cyan Direct USB Accent
    onPrimary = Color(0xFF00262B),
    primaryContainer = Color(0xFF003B46),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = Color(0xFFFFB300),        // Hi-Res Gold Badge Accent
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFF3E2723),
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = Color(0xFF00E676),         // Bit-Perfect Verified Green
    background = Color(0xFF000000),       // True OLED Black
    onBackground = Color(0xFFF7FAFC),
    surface = Color(0xFF07090E),
    onSurface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFF12161F),   // Sleek Audiophile Card Surface
    onSurfaceVariant = Color(0xFFA0AEC0),
    error = Color(0xFFFF5252)
)

@Composable
fun OpenAudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledAudiophileColorScheme,
        content = content
    )
}
