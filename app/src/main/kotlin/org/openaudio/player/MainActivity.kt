/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.openaudio.data.repository.OpenAudioRepository
import org.openaudio.player.navigation.OpenAudioAppScaffold
import org.openaudio.player.ui.theme.OpenAudioTheme

/**
 * Thin Activity entry point hosting the Jetpack Compose navigation graph.
 * All audio engine, USB descriptor parsing, and data management reside in dedicated modules.
 */
class MainActivity : ComponentActivity() {
    private val repository by lazy { OpenAudioRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenAudioTheme {
                OpenAudioAppScaffold(repository = repository)
            }
        }
    }
}
