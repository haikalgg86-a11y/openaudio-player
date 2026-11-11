/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSessionState(
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val positionMs: Long = 0L
)

class OpenAudioMediaSessionController {
    private val _sessionState = MutableStateFlow(PlaybackSessionState())
    val sessionState: StateFlow<PlaybackSessionState> = _sessionState.asStateFlow()
}
