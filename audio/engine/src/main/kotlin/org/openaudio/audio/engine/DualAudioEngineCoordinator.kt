/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openaudio.core.model.AudioOutputPath
import org.openaudio.core.model.AudioSourceFormat
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.BitPerfectVerificationLevel
import org.openaudio.core.model.EndpointSyncType
import org.openaudio.core.model.FeedbackFormatType
import org.openaudio.usb.transfer.UsbInterfaceClaimCoordinator

/**
 * Coordinates switching and live telemetry between Path A (Android Native USB Audio)
 * and Path B (Direct USB Audio Engine).
 */
class DualAudioEngineCoordinator {
    private val _telemetry = MutableStateFlow(
        AudioTelemetrySnapshot(
            activePath = AudioOutputPath.PATH_B_DIRECT_USB,
            verificationLevel = BitPerfectVerificationLevel.HARDWARE_OUTPUT_UNVERIFIED,
            sourceFormat = AudioSourceFormat.FLAC,
            sourceSampleRateHz = 96000,
            sourceBitDepth = 24,
            outputSampleRateHz = 96000,
            outputBitDepth = 24,
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
}
