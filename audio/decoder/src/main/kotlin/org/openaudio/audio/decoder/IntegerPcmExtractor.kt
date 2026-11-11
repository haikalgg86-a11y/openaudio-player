/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.decoder

import org.openaudio.core.model.AudioSourceFormat

/**
 * Contract for bit-accurate integer PCM and DoP/DSD decoding without float conversion.
 */
interface IntegerPcmExtractor {
    val supportedFormat: AudioSourceFormat
    fun isIntegerPathGuaranteed(): Boolean = true
}
