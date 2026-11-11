/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.library

import android.content.ContentResolver
import android.provider.MediaStore
import org.openaudio.core.model.AudioSourceFormat
import org.openaudio.core.model.AudioTrackItem

object LocalMusicScanner {
    val supportedExtensions: Map<String, AudioSourceFormat> = mapOf(
        "flac" to AudioSourceFormat.FLAC,
        "wav" to AudioSourceFormat.WAV,
        "m4a" to AudioSourceFormat.ALAC,
        "alac" to AudioSourceFormat.ALAC,
        "aif" to AudioSourceFormat.AIFF,
        "aiff" to AudioSourceFormat.AIFF,
        "dsf" to AudioSourceFormat.DSF,
        "dff" to AudioSourceFormat.DFF,
        "mp3" to AudioSourceFormat.MP3,
        "aac" to AudioSourceFormat.AAC,
        "ogg" to AudioSourceFormat.OGG_VORBIS,
        "opus" to AudioSourceFormat.OPUS
    )

    fun queryMediaStoreAudio(contentResolver: ContentResolver): List<AudioTrackItem> {
        val tracks = mutableListOf<AudioTrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        runCatching {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val ext = path.substringAfterLast('.', "").lowercase()
                    val fmt = supportedExtensions[ext] ?: AudioSourceFormat.FLAC
                    tracks.add(
                        AudioTrackItem(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: "Unknown Title",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            filePath = path,
                            format = fmt,
                            sampleRateHz = if (fmt == AudioSourceFormat.DSF || fmt == AudioSourceFormat.DFF) 2822400 else 96000,
                            bitDepth = if (fmt == AudioSourceFormat.DSF || fmt == AudioSourceFormat.DFF) 1 else 24,
                            channels = 2,
                            durationMs = cursor.getLong(durCol)
                        )
                    )
                }
            }
        }
        return tracks
    }
}
