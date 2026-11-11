/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.native.audio

object NativeAudioBufferBridge {
    init {
        runCatching { System.loadLibrary("openaudio_audio") }
    }

    external fun nativeGetRuntimePageSize(): Int
}
