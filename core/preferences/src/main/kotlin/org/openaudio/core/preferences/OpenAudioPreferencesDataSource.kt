/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.core.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.openaudio.core.model.AppSettingsState
import org.openaudio.core.model.AudioOutputPath

/**
 * Manages application audio engine and bit-perfect preferences.
 * Enforces the rule: enabling Bit-Perfect Mode automatically disables software EQ, DSP, and crossfade.
 */
class OpenAudioPreferencesDataSource {
    private val _settingsFlow = MutableStateFlow(AppSettingsState())
    val settingsFlow: StateFlow<AppSettingsState> = _settingsFlow.asStateFlow()

    fun setPreferredOutputPath(path: AudioOutputPath) {
        _settingsFlow.update { it.copy(preferredOutputPath = path) }
    }

    fun setBitPerfectModeLocked(enabled: Boolean) {
        _settingsFlow.update { current ->
            if (enabled) {
                current.copy(
                    bitPerfectModeLocked = true,
                    dspEqualizerEnabled = false,
                    replayGainEnabled = false,
                    crossfadeDurationMs = 0
                )
            } else {
                current.copy(bitPerfectModeLocked = false)
            }
        }
    }

    fun setDspEqualizerEnabled(enabled: Boolean) {
        _settingsFlow.update { current ->
            if (enabled) {
                current.copy(
                    dspEqualizerEnabled = true,
                    bitPerfectModeLocked = false
                )
            } else {
                current.copy(dspEqualizerEnabled = false)
            }
        }
    }

    fun setUrbRingCount(count: Int) {
        _settingsFlow.update { it.copy(urbRingCount = count.coerceIn(16, 80)) }
    }
}
