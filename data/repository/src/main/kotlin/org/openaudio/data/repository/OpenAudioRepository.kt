/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openaudio.audio.engine.DualAudioEngineCoordinator
import org.openaudio.core.common.OpenAudioResult
import org.openaudio.core.model.AudioTrackItem
import org.openaudio.core.preferences.OpenAudioPreferencesDataSource
import org.openaudio.player.library.LocalMusicScanner
import org.openaudio.usb.detection.UsbDacDetector
import org.openaudio.usb.transfer.UsbHardwareSessionController
import org.openaudio.usb.transfer.UsbInterfaceClaimCoordinator
import java.io.File

class OpenAudioRepository(
    val preferences: OpenAudioPreferencesDataSource = OpenAudioPreferencesDataSource(),
    val usbDetector: UsbDacDetector = UsbDacDetector(),
    val audioCoordinator: DualAudioEngineCoordinator = DualAudioEngineCoordinator()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val feasibilityMatrix = UsbInterfaceClaimCoordinator.getPhase11FeasibilityMatrix()

    private val _scannedTracks = MutableStateFlow<List<AudioTrackItem>>(emptyList())
    val scannedTracks: StateFlow<List<AudioTrackItem>> = _scannedTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("Ketuk 'Scan Otomatis Lagu di HP' atau 'Pilih Folder Musik' untuk memuat lagu Anda.")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrackItem?>(null)
    val currentTrack: StateFlow<AudioTrackItem?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatAllEnabled = MutableStateFlow(true)
    val repeatAllEnabled: StateFlow<Boolean> = _repeatAllEnabled.asStateFlow()

    private var activePlayer: MediaPlayer? = null

    init {
        scope.launch {
            while (true) {
                delay(250L)
                val player = activePlayer
                if (player != null && _isPlaying.value) {
                    runCatching {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition.toLong()
                            val d = player.duration.toLong()
                            if (d > 0) _durationMs.value = d
                        }
                    }
                }
            }
        }
    }

    fun scanDeviceMediaStore(context: Context) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanStatusMessage.value = "Sedang memindai seluruh lagu di penyimpanan HP & mengecek resolusi bit/kHz..."
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                LocalMusicScanner.queryMediaStoreAudio(context.contentResolver)
            }
            _scannedTracks.value = found
            _isScanning.value = false
            _scanStatusMessage.value = if (found.isEmpty()) {
                "Tidak ada lagu yang terdeteksi otomatis. Silakan ketuk tombol 'Pilih Folder Musik' di sebelah kanan."
            } else {
                "Berhasil menemukan ${found.size} lagu siap putar."
            }
        }
    }

    fun scanSafFolder(context: Context, treeUri: Uri) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanStatusMessage.value = "Sedang membaca file musik di dalam folder pilihan Anda..."
        scope.launch {
            val folderTracks = withContext(Dispatchers.IO) {
                LocalMusicScanner.scanSafDocumentTree(context.contentResolver, treeUri)
            }
            val combined = (_scannedTracks.value + folderTracks).distinctBy { it.filePath }
            _scannedTracks.value = combined
            _isScanning.value = false
            _scanStatusMessage.value = "Berhasil menambahkan ${folderTracks.size} lagu dari folder (Total: ${combined.size} lagu)."
        }
    }

    fun selectAndPlayTrack(context: Context, track: AudioTrackItem) {
        _currentTrack.value = track
        _currentPositionMs.value = 0L
        _durationMs.value = track.durationMs.coerceAtLeast(1000L)

        val settings = preferences.settingsFlow.value
        audioCoordinator.onTrackLoaded(
            track = track,
            dac = usbDetector.connectedDac.value,
            dspBypassed = settings.bitPerfectModeLocked,
            activePath = settings.preferredOutputPath
        )

        runCatching {
            activePlayer?.release()
            activePlayer = MediaPlayer().apply {
                if (track.filePath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(track.filePath))
                } else if (File(track.filePath).exists()) {
                    setDataSource(track.filePath)
                }
                prepare()
                val actualDur = duration.toLong()
                if (actualDur > 0) {
                    _durationMs.value = actualDur
                }
                start()
                setOnCompletionListener {
                    skipToNext(context)
                }
            }
            _isPlaying.value = true
        }.onFailure {
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = activePlayer ?: return
        runCatching {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
            }
        }
    }

    fun seekToPosition(positionMs: Long) {
        val player = activePlayer ?: return
        runCatching {
            player.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun skipToNext(context: Context) {
        val list = _scannedTracks.value
        if (list.isEmpty()) return
        val cur = _currentTrack.value
        val nextTrack = if (_shuffleEnabled.value) {
            list.random()
        } else {
            val idx = list.indexOfFirst { it.id == cur?.id }
            if (idx in 0 until list.lastIndex) {
                list[idx + 1]
            } else if (_repeatAllEnabled.value) {
                list.first()
            } else {
                _isPlaying.value = false
                return
            }
        }
        selectAndPlayTrack(context, nextTrack)
    }

    fun skipToPrevious(context: Context) {
        val list = _scannedTracks.value
        if (list.isEmpty()) return
        if (_currentPositionMs.value > 4000L) {
            seekToPosition(0L)
            return
        }
        val cur = _currentTrack.value
        val idx = list.indexOfFirst { it.id == cur?.id }
        val prevTrack = if (idx > 0) list[idx - 1] else list.last()
        selectAndPlayTrack(context, prevTrack)
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatAllEnabled.value = !_repeatAllEnabled.value
    }

    fun scanAndConnectUsbDac(context: Context) {
        val usbController = UsbHardwareSessionController(context)
        val devices = usbController.scanAttachedAudioDevices()
        val firstDac = devices.firstOrNull()
        if (firstDac == null) {
            usbDetector.onDacDetached()
            return
        }
        if (!usbController.hasPermission(firstDac)) {
            val initialInfo = usbController.inspectAndOpenDac(firstDac)
            if (initialInfo is OpenAudioResult.Success) {
                usbDetector.onDacAttached(initialInfo.data)
            }
            usbController.requestUsbPermission(firstDac)
        } else {
            when (val res = usbController.inspectAndOpenDac(firstDac)) {
                is OpenAudioResult.Success -> {
                    usbDetector.onDacAttached(res.data)
                    _currentTrack.value?.let { track ->
                        val settings = preferences.settingsFlow.value
                        audioCoordinator.onTrackLoaded(
                            track = track,
                            dac = res.data,
                            dspBypassed = settings.bitPerfectModeLocked,
                            activePath = settings.preferredOutputPath
                        )
                    }
                }
                is OpenAudioResult.Error -> usbDetector.onDacDetached()
            }
        }
    }
}
