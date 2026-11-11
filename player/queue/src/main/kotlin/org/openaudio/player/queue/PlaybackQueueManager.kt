/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openaudio.core.model.AudioTrackItem

class PlaybackQueueManager {
    private val _queue = MutableStateFlow<List<AudioTrackItem>>(emptyList())
    val queue: StateFlow<List<AudioTrackItem>> = _queue.asStateFlow()
}
