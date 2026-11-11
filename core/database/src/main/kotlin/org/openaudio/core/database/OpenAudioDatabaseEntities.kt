/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.core.database

import org.openaudio.core.model.AudioSourceFormat
import org.openaudio.core.model.AudioTrackItem

data class TrackEntity(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,
    val formatName: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channels: Int,
    val durationMs: Long
) {
    fun toDomainModel(): AudioTrackItem = AudioTrackItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        filePath = filePath,
        format = runCatching { AudioSourceFormat.valueOf(formatName) }.getOrDefault(AudioSourceFormat.FLAC),
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        channels = channels,
        durationMs = durationMs
    )
}

data class KnownDacProfileEntity(
    val vidPid: String,
    val vendorId: Int,
    val productId: Int,
    val productName: String,
    val preferredPathName: String,
    val lastVerifiedLevelCode: String,
    val detectedFeedbackFormat: String
)
