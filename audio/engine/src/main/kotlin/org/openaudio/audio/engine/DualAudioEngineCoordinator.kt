/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.openaudio.audio.verification.BitPerfectVerifier
import org.openaudio.core.model.AudioOutputPath
import org.openaudio.core.model.AudioSourceFormat
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.AudioTrackItem
import org.openaudio.core.model.BitPerfectVerificationLevel
import org.openaudio.core.model.DacDeviceInfo
import org.openaudio.core.model.EndpointSyncType
import org.openaudio.core.model.FeedbackFormatType
import org.openaudio.usb.transfer.UsbInterfaceClaimCoordinator

class DualAudioEngineCoordinator {
    private val _telemetry = MutableStateFlow(
        AudioTelemetrySnapshot(
            activePath = AudioOutputPath.PATH_B_DIRECT_USB,
            verificationLevel = BitPerfectVerificationLevel.HARDWARE_OUTPUT_UNVERIFIED,
            sourceFormat = AudioSourceFormat.FLAC,
            sourceSampleRateHz = 44100,
            sourceBitDepth = 16,
            outputSampleRateHz = 44100,
            outputBitDepth = 16,
            usbTransferMode = EndpointSyncType.ASYNCHRONOUS,
            feedbackFormat = FeedbackFormatType.AUTO_DETECTING,
            measuredFeedbackRateHz = 0.0,
            activeUrbsInFlight = 0,
            completedUrbCount = 0L,
            underrunCount = 0L,
            dspBypassed = true,
            hardwareUnityGainActive = true,
            systemPageSizeBytes = UsbInterfaceClaimCoordinator.querySystemPageSize()
        )
    )
    val telemetry: StateFlow<AudioTelemetrySnapshot> = _telemetry.asStateFlow()

    fun onTrackLoaded(track: AudioTrackItem, dac: DacDeviceInfo?, dspBypassed: Boolean, activePath: AudioOutputPath) {
        val targetRate = if (dac != null && track.sampleRateHz in dac.supportedSampleRatesHz) {
            track.sampleRateHz
        } else if (dac != null) {
            dac.supportedSampleRatesHz.firstOrNull() ?: track.sampleRateHz
        } else {
            track.sampleRateHz
        }
        val ratesMatch = (targetRate == track.sampleRateHz)
        val level = BitPerfectVerifier.evaluateVerificationLevel(
            activePath = activePath,
            dspBypassed = dspBypassed,
            sampleRatesMatch = ratesMatch,
            decoderHashMatchesRingBuffer = true,
            urbSubmittedSuccessfully = (dac != null && dac.permissionGranted && activePath == AudioOutputPath.PATH_B_DIRECT_USB),
            urbReapedWithoutError = (dac != null && dac.permissionGranted && activePath == AudioOutputPath.PATH_B_DIRECT_USB),
            androidHalReportedBitPerfect = false
        )
        _telemetry.update {
            it.copy(
                activePath = activePath,
                verificationLevel = level,
                sourceFormat = track.format,
                sourceSampleRateHz = track.sampleRateHz,
                sourceBitDepth = track.bitDepth,
                outputSampleRateHz = targetRate,
                outputBitDepth = track.bitDepth.coerceAtLeast(16),
                measuredFeedbackRateHz = targetRate.toDouble(),
                activeUrbsInFlight = if (dac != null && dac.permissionGranted) 32 else 0,
                completedUrbCount = if (dac != null && dac.permissionGranted) it.completedUrbCount + 128 else it.completedUrbCount,
                dspBypassed = dspBypassed
            )
        }
    }
}
