/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Pure Dark AMOLED Material 3 Color Palette designed for minimalist audiophile clarity.
 */
private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF001E36),
    primaryContainer = Color(0xFF0D2B45),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF002022),
    background = Color(0xFF000000),      // True AMOLED Black
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF000000),         // True AMOLED Black
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF12151A),  // Elevated Audiophile Slate Card
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = Color(0xFFEF5350)
)

@Composable
fun OpenAudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledDarkColorScheme,
        content = content
    )
}
