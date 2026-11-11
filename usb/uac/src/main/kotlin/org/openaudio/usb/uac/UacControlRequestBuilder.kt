/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.usb.uac

/**
 * Builds standard USB Audio Class 1.0 and 2.0 Control Setup Packets
 * for sample rate configuration and hardware volume (Feature Unit).
 */
object UacControlRequestBuilder {
    const val USB_DIR_OUT_CLASS_INTERFACE = 0x21
    const val USB_DIR_IN_CLASS_INTERFACE = 0xA1
    const val USB_DIR_OUT_CLASS_ENDPOINT = 0x22
    const val USB_DIR_IN_CLASS_ENDPOINT = 0xA2

    // UAC 1.0 Endpoint Sample Rate 3-byte Little-Endian Payload
    fun encodeUac1SampleRatePayload(sampleRateHz: Int): ByteArray = byteArrayOf(
        (sampleRateHz and 0xFF).toByte(),
        ((sampleRateHz shr 8) and 0xFF).toByte(),
        ((sampleRateHz shr 16) and 0xFF).toByte()
    )

    // UAC 2.0 Clock Source Sample Rate 4-byte Little-Endian Payload
    fun encodeUac2SampleRatePayload(sampleRateHz: Int): ByteArray = byteArrayOf(
        (sampleRateHz and 0xFF).toByte(),
        ((sampleRateHz shr 8) and 0xFF).toByte(),
        ((sampleRateHz shr 16) and 0xFF).toByte(),
        ((sampleRateHz shr 24) and 0xFF).toByte()
    )
}
