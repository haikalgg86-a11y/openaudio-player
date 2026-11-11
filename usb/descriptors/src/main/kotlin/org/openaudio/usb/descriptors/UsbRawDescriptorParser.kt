/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.usb.descriptors

import org.openaudio.core.model.EndpointSyncType
import org.openaudio.core.model.UacVersion

data class ParsedUsbAudioTopology(
    val uacVersion: UacVersion,
    val audioControlInterface: Int,
    val audioStreamingInterfaces: List<Int>,
    val isochronousOutEndpoint: Int,
    val feedbackInEndpoint: Int?,
    val syncType: EndpointSyncType,
    val sampleRatesHz: List<Int>,
    val bitDepths: List<Int>,
    val maxChannels: Int,
    val clockSourceId: Int?,
    val clockSelectorId: Int?,
    val supportsHardwareVolume: Boolean
)

/**
 * Bounds-checked binary USB descriptor parser for UAC 1.0 and UAC 2.0 raw descriptors
 * returned by UsbDeviceConnection.getRawDescriptors().
 */
object UsbRawDescriptorParser {
    private const val USB_DT_INTERFACE = 0x04
    private const val USB_DT_ENDPOINT = 0x05
    private const val USB_DT_CS_INTERFACE = 0x24
    private const val USB_CLASS_AUDIO = 0x01
    private const val USB_SUBCLASS_AUDIOCONTROL = 0x01
    private const val USB_SUBCLASS_AUDIOSTREAMING = 0x02

    fun parseRawDescriptors(raw: ByteArray): ParsedUsbAudioTopology {
        var offset = 0
        var uacVersion = UacVersion.UNKNOWN
        var acInterface = 0
        val asInterfaces = mutableSetOf<Int>()
        var isoOutEp = 0x01
        var feedbackInEp: Int? = null
        var syncType = EndpointSyncType.ASYNCHRONOUS
        val sampleRates = sortedSetOf<Int>()
        val bitDepths = sortedSetOf<Int>()
        var maxChannels = 2
        var clockSourceId: Int? = null
        var clockSelectorId: Int? = null
        var supportsHwVolume = false
        var currentInterfaceSubClass = 0

        while (offset + 2 <= raw.size) {
            val bLength = raw[offset].toInt() and 0xFF
            val bDescriptorType = raw[offset + 1].toInt() and 0xFF
            if (bLength < 2 || offset + bLength > raw.size) {
                break // Prevent malformed descriptor over-read
            }

            when (bDescriptorType) {
                USB_DT_INTERFACE -> if (bLength >= 9) {
                    val intfNum = raw[offset + 2].toInt() and 0xFF
                    val intfClass = raw[offset + 5].toInt() and 0xFF
                    val intfSubClass = raw[offset + 6].toInt() and 0xFF
                    val intfProtocol = raw[offset + 7].toInt() and 0xFF
                    currentInterfaceSubClass = intfSubClass

                    if (intfClass == USB_CLASS_AUDIO) {
                        uacVersion = if (intfProtocol == 0x20) UacVersion.UAC_2_0 else UacVersion.UAC_1_0
                        if (intfSubClass == USB_SUBCLASS_AUDIOCONTROL) {
                            acInterface = intfNum
                        } else if (intfSubClass == USB_SUBCLASS_AUDIOSTREAMING) {
                            asInterfaces.add(intfNum)
                        }
                    }
                }
                USB_DT_CS_INTERFACE -> if (bLength >= 4) {
                    val subtype = raw[offset + 2].toInt() and 0xFF
                    if (currentInterfaceSubClass == USB_SUBCLASS_AUDIOCONTROL) {
                        when (subtype) {
                            0x06 -> supportsHwVolume = true // FEATURE_UNIT
                            0x0A -> if (uacVersion == UacVersion.UAC_2_0) {
                                clockSourceId = raw[offset + 3].toInt() and 0xFF
                            }
                            0x0B -> if (uacVersion == UacVersion.UAC_2_0) {
                                clockSelectorId = raw[offset + 3].toInt() and 0xFF
                            }
                        }
                    } else if (currentInterfaceSubClass == USB_SUBCLASS_AUDIOSTREAMING && subtype == 0x02) {
                        // FORMAT_TYPE descriptor
                        if (uacVersion == UacVersion.UAC_1_0 && bLength >= 8) {
                            val ch = raw[offset + 4].toInt() and 0xFF
                            val bitRes = raw[offset + 6].toInt() and 0xFF
                            val samFreqType = raw[offset + 7].toInt() and 0xFF
                            if (ch > maxChannels) maxChannels = ch
                            if (bitRes in listOf(16, 24, 32)) bitDepths.add(bitRes)
                            if (samFreqType > 0) {
                                for (i in 0 until samFreqType) {
                                    val idx = offset + 8 + (i * 3)
                                    if (idx + 3 <= offset + bLength) {
                                        val freq = (raw[idx].toInt() and 0xFF) or
                                            ((raw[idx + 1].toInt() and 0xFF) shl 8) or
                                            ((raw[idx + 2].toInt() and 0xFF) shl 16)
                                        if (freq in 8000..768000) sampleRates.add(freq)
                                    }
                                }
                            }
                        } else if (uacVersion == UacVersion.UAC_2_0 && bLength >= 6) {
                            val bitRes = raw[offset + 5].toInt() and 0xFF
                            if (bitRes in listOf(16, 24, 32)) bitDepths.add(bitRes)
                        }
                    }
                }
                USB_DT_ENDPOINT -> if (bLength >= 7) {
                    val epAddr = raw[offset + 2].toInt() and 0xFF
                    val bmAttr = raw[offset + 3].toInt() and 0xFF
                    val xferType = bmAttr and 0x03
                    if (xferType == 0x01) { // Isochronous
                        val isDirIn = (epAddr and 0x80) != 0
                        if (!isDirIn) {
                            isoOutEp = epAddr
                            syncType = when ((bmAttr shr 2) and 0x03) {
                                0x01 -> EndpointSyncType.ASYNCHRONOUS
                                0x02 -> EndpointSyncType.ADAPTIVE
                                0x03 -> EndpointSyncType.SYNCHRONOUS
                                else -> EndpointSyncType.NONE
                            }
                        } else {
                            feedbackInEp = epAddr
                        }
                    }
                }
            }
            offset += bLength
        }

        if (bitDepths.isEmpty()) bitDepths.addAll(listOf(16, 24))
        if (sampleRates.isEmpty()) sampleRates.addAll(listOf(44100, 48000, 88200, 96000, 176400, 192000))

        return ParsedUsbAudioTopology(
            uacVersion = uacVersion,
            audioControlInterface = acInterface,
            audioStreamingInterfaces = asInterfaces.toList().ifEmpty { listOf(1) },
            isochronousOutEndpoint = isoOutEp,
            feedbackInEndpoint = feedbackInEp,
            syncType = syncType,
            sampleRatesHz = sampleRates.toList(),
            bitDepths = bitDepths.toList(),
            maxChannels = maxChannels,
            clockSourceId = clockSourceId,
            clockSelectorId = clockSelectorId,
            supportsHardwareVolume = supportsHwVolume
        )
    }
}
