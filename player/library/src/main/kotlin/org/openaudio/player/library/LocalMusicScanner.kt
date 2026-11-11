/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.library

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import org.openaudio.core.model.AudioSourceFormat
import org.openaudio.core.model.AudioTrackItem
import org.openaudio.ndk.decoder.NativeDecoderBridge
import java.io.File

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

    /**
     * Scans all local music indexed by Android MediaStore and probes file headers via Native C++17
     * to read true sampleRateHz and bitDepth (FLAC STREAMINFO / WAV fmt / DSF fmt).
     */
    fun queryMediaStoreAudio(contentResolver: ContentResolver): List<AudioTrackItem> {
        val tracks = mutableListOf<AudioTrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
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
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: ""
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val ext = (rawPath ?: displayName).substringAfterLast('.', "").lowercase()
                    val fmt = supportedExtensions[ext] ?: AudioSourceFormat.FLAC
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val (sampleRate, bitDepth, channels) = probeStreamParams(contentResolver, contentUri, rawPath, fmt)

                    tracks.add(
                        AudioTrackItem(
                            id = id,
                            title = cursor.getString(titleCol) ?: displayName.substringBeforeLast('.'),
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            filePath = rawPath ?: contentUri.toString(),
                            format = fmt,
                            sampleRateHz = sampleRate,
                            bitDepth = bitDepth,
                            channels = channels,
                            durationMs = cursor.getLong(durCol)
                        )
                    )
                }
            }
        }
        return tracks
    }

    /**
     * Traverses a user-selected SAF Directory Tree (OpenDocumentTree) to find FLAC, WAV, DSF, DFF, and lossy files
     * even if Android MediaStore did not index them.
     */
    fun scanSafDocumentTree(contentResolver: ContentResolver, treeUri: Uri): List<AudioTrackItem> {
        val results = mutableListOf<AudioTrackItem>()
        runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                var nextId = 100000L
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    val mime = cursor.getString(2) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue

                    val ext = name.substringAfterLast('.', "").lowercase()
                    val fmt = supportedExtensions[ext] ?: continue
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val (sampleRate, bitDepth, channels) = probeStreamParams(contentResolver, fileUri, null, fmt)

                    results.add(
                        AudioTrackItem(
                            id = nextId++,
                            title = name.substringBeforeLast('.'),
                            artist = "Folder Track",
                            album = rootDocId.substringAfterLast('/'),
                            filePath = fileUri.toString(),
                            format = fmt,
                            sampleRateHz = sampleRate,
                            bitDepth = bitDepth,
                            channels = channels,
                            durationMs = 0L
                        )
                    )
                }
            }
        }
        return results
    }

    private fun probeStreamParams(
        contentResolver: ContentResolver,
        uri: Uri,
        filePath: String?,
        fallbackFormat: AudioSourceFormat
    ): Triple<Int, Int, Int> {
        runCatching {
            val header = ByteArray(128)
            val bytesRead = if (filePath != null && File(filePath).canRead()) {
                File(filePath).inputStream().use { it.read(header) }
            } else {
                contentResolver.openInputStream(uri)?.use { it.read(header) } ?: -1
            }
            if (bytesRead >= 42) {
                val probed = NativeDecoderBridge.nativeProbeHeader(header, bytesRead)
                if (probed != null && probed.size >= 5 && probed[0] == 1) {
                    val sr = probed[2].takeIf { it > 0 } ?: 44100
                    val ch = probed[3].takeIf { it > 0 } ?: 2
                    val bd = probed[4].takeIf { it > 0 } ?: 16
                    return Triple(sr, bd, ch)
                }
            }
        }
        return when (fallbackFormat) {
            AudioSourceFormat.DSF, AudioSourceFormat.DFF -> Triple(2822400, 1, 2)
            AudioSourceFormat.FLAC, AudioSourceFormat.WAV, AudioSourceFormat.ALAC, AudioSourceFormat.AIFF -> Triple(44100, 16, 2)
            else -> Triple(44100, 16, 2)
        }
    }
}
