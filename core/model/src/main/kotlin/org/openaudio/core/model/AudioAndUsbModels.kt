/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.core.model

/**
 * 5-Level Transparent Bit-Perfect Verification System defined in Phase 1.1.
 * Never claims hardware-internal bit-perfect status solely from host URB completion.
 */
enum class BitPerfectVerificationLevel(
    val code: String,
    val title: String,
    val caveatDescription: String
) {
    PCM_PIPELINE_VERIFIED(
        code = "LEVEL_A",
        title = "A. PCM Pipeline Verified",
        caveatDescription = "Integer decoder output matches ring buffer output (XXH64/CRC32 verified, DSP & software volume bypassed). Transport not yet verified."
    ),
    USB_URB_SUBMISSION_VERIFIED(
        code = "LEVEL_B",
        title = "B. USB URB Submission Verified",
        caveatDescription = "Exclusive USB interface claimed, DAC clock configured, and identical integer PCM frames queued via USBDEVFS_SUBMITURB."
    ),
    USB_TRANSFER_COMPLETION_VERIFIED(
        code = "LEVEL_C",
        title = "C. USB Transfer Completion Verified",
        caveatDescription = "xHCI host controller completed all isochronous packets via USBDEVFS_REAPURB with status == 0 and actual_length == length. Internal DAC silicon processing cannot be inspected from the USB host bus."
    ),
    ANDROID_HAL_BIT_PERFECT_REPORTED(
        code = "LEVEL_D",
        title = "D. Android HAL BIT_PERFECT Reported",
        caveatDescription = "Android 14–16 AudioManager confirmed MIXER_BEHAVIOR_BIT_PERFECT on Path A. Vendor AIDL Audio HAL internal pipeline is trusted as reported by OS."
    ),
    HARDWARE_OUTPUT_UNVERIFIED(
        code = "LEVEL_E",
        title = "E. Hardware Output Unverified",
        caveatDescription = "End-to-end physical DAC output (I2S/S/PDIF) requires an external digital loopback capture device for verification, or OS is resampling."
    )
}

enum class FeasibilityCategory(val label: String) {
    CONFIRMED_BY_OFFICIAL_DOCS("Confirmed by official documentation"),
    REQUIRES_DEVICE_TESTING("Requires device testing"),
    KERNEL_DEPENDENT("Kernel dependent"),
    ROM_DEPENDENT("ROM dependent"),
    NOT_SUPPORTED("Not supported")
}

enum class AudioOutputPath(val displayName: String) {
    PATH_A_ANDROID_NATIVE("Path A — Android Native USB Audio (AudioTrack / PreferredMixerAttributes)"),
    PATH_B_DIRECT_USB("Path B — Direct USB Audio Engine (Native C++17 / Linux USBDEVFS Isochronous)")
}

enum class UacVersion(val label: String, val protocolCode: Int) {
    UAC_1_0("USB Audio Class 1.0", 0x00),
    UAC_2_0("USB Audio Class 2.0", 0x20),
    UNKNOWN("Unknown / Non-Standard UAC", -1)
}

enum class EndpointSyncType(val label: String) {
    ASYNCHRONOUS("Asynchronous (DAC Master Clock + Feedback)"),
    ADAPTIVE("Adaptive (Host Clock Driven)"),
    SYNCHRONOUS("Synchronous (USB SOF Locked)"),
    NONE("None")
}

enum class FeedbackFormatType(val label: String) {
    FORMAT_10_14_3BYTE("10.14 Fixed-Point (3-Byte Full-Speed Standard)"),
    FORMAT_16_16_4BYTE("16.16 Fixed-Point (4-Byte High-Speed Standard)"),
    FORMAT_10_14_4BYTE_QUIRK("10.14 Fixed-Point in 4-Byte (High-Speed Quirk)"),
    FORMAT_12_13_QUIRK("12.13 Fixed-Point (Legacy Quirk)"),
    AUTO_DETECTING("Auto-Detecting via Q16.16 Shift Window"),
    NONE("No Feedback Endpoint (Adaptive/Sync)")
}

enum class AudioSourceFormat(val label: String, val isLossless: Boolean) {
    FLAC("FLAC", true),
    WAV("WAV PCM", true),
    ALAC("ALAC", true),
    AIFF("AIFF", true),
    DSF("DSD (.dsf)", true),
    DFF("DSD (.dff)", true),
    MP3("MP3", false),
    AAC("AAC", false),
    OGG_VORBIS("Ogg Vorbis", false),
    OPUS("Opus", false)
}

data class DacDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String?,
    val productName: String?,
    val deviceNamePath: String,
    val uacVersion: UacVersion,
    val usbSpeedLabel: String,
    val audioControlInterfaceNumber: Int,
    val audioStreamingInterfaceNumbers: List<Int>,
    val isochronousOutEndpointAddress: Int,
    val feedbackInEndpointAddress: Int?,
    val endpointSyncType: EndpointSyncType,
    val supportedSampleRatesHz: List<Int>,
    val supportedBitDepths: List<Int>,
    val maxChannels: Int,
    val clockSourceId: Int?,
    val clockSelectorId: Int?,
    val supportsHardwareVolume: Boolean,
    val kernelDriverBound: String?,
    val permissionGranted: Boolean
) {
    val formattedVidPid: String
        get() = String.format("%04X:%04X", vendorId, productId)
    val safeManufacturer: String
        get() = manufacturerName?.takeIf { it.isNotBlank() } ?: "Unknown Manufacturer (No USB String)"
    val safeProduct: String
        get() = productName?.takeIf { it.isNotBlank() } ?: "Generic USB Audio DAC ($formattedVidPid)"
}

data class AudioTelemetrySnapshot(
    val activePath: AudioOutputPath,
    val verificationLevel: BitPerfectVerificationLevel,
    val sourceFormat: AudioSourceFormat,
    val sourceSampleRateHz: Int,
    val sourceBitDepth: Int,
    val outputSampleRateHz: Int,
    val outputBitDepth: Int,
    val usbTransferMode: EndpointSyncType,
    val feedbackFormat: FeedbackFormatType,
    val measuredFeedbackRateHz: Double,
    val activeUrbsInFlight: Int,
    val completedUrbCount: Long,
    val underrunCount: Long,
    val dspBypassed: Boolean,
    val hardwareUnityGainActive: Boolean,
    val systemPageSizeBytes: Int
)

data class AudioTrackItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,
    val format: AudioSourceFormat,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channels: Int,
    val durationMs: Long
)

data class AppSettingsState(
    val preferredOutputPath: AudioOutputPath = AudioOutputPath.PATH_B_DIRECT_USB,
    val bitPerfectModeLocked: Boolean = true,
    val autoSampleRateSwitching: Boolean = true,
    val urbRingCount: Int = 48,
    val microframesPerUrb: Int = 8,
    val enableRamMlock: Boolean = true,
    val enableHardwareVolume: Boolean = true,
    val dspEqualizerEnabled: Boolean = false,
    val replayGainEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 0
)
