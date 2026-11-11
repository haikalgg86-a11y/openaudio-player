/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface OpenAudioResult<out T> {
    data class Success<T>(val data: T) : OpenAudioResult<T>
    data class Error(
        val message: String,
        val errno: Int? = null,
        val cause: Throwable? = null
    ) : OpenAudioResult<Nothing>
}

data class OpenAudioDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val computation: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main
)
