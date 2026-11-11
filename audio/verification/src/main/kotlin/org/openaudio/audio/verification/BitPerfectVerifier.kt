/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.verification

import org.openaudio.core.model.AudioOutputPath
import org.openaudio.core.model.BitPerfectVerificationLevel

/**
 * Evaluates the current audio signal path against the 5-level verification hierarchy
 * defined in Phase 1.1. Never reports hardware verification without proof.
 */
object BitPerfectVerifier {
    fun evaluateVerificationLevel(
        activePath: AudioOutputPath,
        dspBypassed: Boolean,
        sampleRatesMatch: Boolean,
        decoderHashMatchesRingBuffer: Boolean,
        urbSubmittedSuccessfully: Boolean,
        urbReapedWithoutError: Boolean,
        androidHalReportedBitPerfect: Boolean
    ): BitPerfectVerificationLevel {
        if (!dspBypassed || !sampleRatesMatch || !decoderHashMatchesRingBuffer) {
            return BitPerfectVerificationLevel.HARDWARE_OUTPUT_UNVERIFIED
        }

        return when (activePath) {
            AudioOutputPath.PATH_B_DIRECT_USB -> {
                when {
                    urbReapedWithoutError -> BitPerfectVerificationLevel.USB_TRANSFER_COMPLETION_VERIFIED
                    urbSubmittedSuccessfully -> BitPerfectVerificationLevel.USB_URB_SUBMISSION_VERIFIED
                    else -> BitPerfectVerificationLevel.PCM_PIPELINE_VERIFIED
                }
            }
            AudioOutputPath.PATH_A_ANDROID_NATIVE -> {
                if (androidHalReportedBitPerfect) {
                    BitPerfectVerificationLevel.ANDROID_HAL_BIT_PERFECT_REPORTED
                } else {
                    BitPerfectVerificationLevel.PCM_PIPELINE_VERIFIED
                }
            }
        }
    }
}
