/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.dsp

data class ParametricEqBand(
    val frequencyHz: Float,
    val gainDb: Float,
    val qFactor: Float,
    val enabled: Boolean = true
)

/**
 * Optional 64-bit DSP Pipeline.
 * Strictly bypassed whenever Bit-Perfect Mode is active.
 */
class AudioDspPipeline {
    val defaultBands: List<ParametricEqBand> = listOf(
        ParametricEqBand(31.5f, 0.0f, 1.0f),
        ParametricEqBand(63.0f, 0.0f, 1.0f),
        ParametricEqBand(125.0f, 0.0f, 1.0f),
        ParametricEqBand(250.0f, 0.0f, 1.0f),
        ParametricEqBand(500.0f, 0.0f, 1.0f),
        ParametricEqBand(1000.0f, 0.0f, 1.0f),
        ParametricEqBand(2000.0f, 0.0f, 1.0f),
        ParametricEqBand(4000.0f, 0.0f, 1.0f),
        ParametricEqBand(8000.0f, 0.0f, 1.0f),
        ParametricEqBand(16000.0f, 0.0f, 1.0f)
    )
}
