/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.native.decoder

object NativeDecoderBridge {
    init {
        runCatching { System.loadLibrary("openaudio_decoder") }
    }

    external fun nativeComputePcmHash64(pcmData: ByteArray, length: Int, seed: Long): Long
    external fun nativeProbeHeader(headerBytes: ByteArray, length: Int): IntArray?
}
