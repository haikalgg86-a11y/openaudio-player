/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.audio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.Executors

data class PathANegotiationResult(
    val usbAudioDeviceFound: Boolean,
    val deviceProductName: String,
    val requestedSampleRateHz: Int,
    val requestedEncoding: Int,
    val bitPerfectMixerSupportedByRom: Boolean,
    val bitPerfectMixerActivated: Boolean,
    val fallbackReason: String?
)

/**
 * PHASE 5 — PATH A: ANDROID NATIVE USB AUDIO ENGINE.
 * Uses Android AudioManager, AudioDeviceInfo, and Android 14–16 (API 34–36)
 * PreferredMixerAttributes (MIXER_BEHAVIOR_BIT_PERFECT) when supported by the Custom ROM & Audio HAL.
 * Reports honestly when a Custom ROM falls back to MIXER_BEHAVIOR_DEFAULT.
 */
class AndroidNativeUsbAudioPathA(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val listenerExecutor = Executors.newSingleThreadExecutor()
    private var activeUsbDevice: AudioDeviceInfo? = null
    private var activeAudioAttributes: AudioAttributes? = null

    fun findConnectedUsbAudioOutputDevice(): AudioDeviceInfo? {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
    }

    fun negotiateAndConfigureBitPerfectPathA(
        sampleRateHz: Int,
        bitDepth: Int,
        channelMask: Int = AudioFormat.CHANNEL_OUT_STEREO
    ): PathANegotiationResult {
        val usbDevice = findConnectedUsbAudioOutputDevice()
            ?: return PathANegotiationResult(
                usbAudioDeviceFound = false,
                deviceProductName = "None",
                requestedSampleRateHz = sampleRateHz,
                requestedEncoding = AudioFormat.ENCODING_PCM_16BIT,
                bitPerfectMixerSupportedByRom = false,
                bitPerfectMixerActivated = false,
                fallbackReason = "No USB Audio output device detected by Android AudioManager."
            )

        val targetEncoding = when (bitDepth) {
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_24BIT_PACKED
        }

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val supportedAttrs: List<AudioMixerAttributes> =
                runCatching { audioManager.getSupportedMixerAttributes(usbDevice) }
                    .getOrDefault(emptyList())

            val matchedBitPerfectAttr = supportedAttrs.firstOrNull { attr ->
                attr.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                    attr.format.sampleRate == sampleRateHz &&
                    attr.format.encoding == targetEncoding
            }

            if (matchedBitPerfectAttr != null) {
                val applied = runCatching {
                    audioManager.setPreferredMixerAttributes(
                        audioAttrs,
                        usbDevice,
                        matchedBitPerfectAttr
                    )
                }.getOrDefault(false)

                if (applied) {
                    activeUsbDevice = usbDevice
                    activeAudioAttributes = audioAttrs
                    return PathANegotiationResult(
                        usbAudioDeviceFound = true,
                        deviceProductName = usbDevice.productName?.toString() ?: "USB Audio Device",
                        requestedSampleRateHz = sampleRateHz,
                        requestedEncoding = targetEncoding,
                        bitPerfectMixerSupportedByRom = true,
                        bitPerfectMixerActivated = true,
                        fallbackReason = null
                    )
                } else {
                    return PathANegotiationResult(
                        usbAudioDeviceFound = true,
                        deviceProductName = usbDevice.productName?.toString() ?: "USB Audio Device",
                        requestedSampleRateHz = sampleRateHz,
                        requestedEncoding = targetEncoding,
                        bitPerfectMixerSupportedByRom = true,
                        bitPerfectMixerActivated = false,
                        fallbackReason = "AudioManager.setPreferredMixerAttributes() returned false (HAL rejected configuration)."
                    )
                }
            } else {
                return PathANegotiationResult(
                    usbAudioDeviceFound = true,
                    deviceProductName = usbDevice.productName?.toString() ?: "USB Audio Device",
                    requestedSampleRateHz = sampleRateHz,
                    requestedEncoding = targetEncoding,
                    bitPerfectMixerSupportedByRom = false,
                    bitPerfectMixerActivated = false,
                    fallbackReason = "Custom ROM Audio HAL did not advertise MIXER_BEHAVIOR_BIT_PERFECT for ${bitDepth}-bit / ${sampleRateHz} Hz. Recommend switching to Path B (Direct USB Engine)."
                )
            }
        }

        return PathANegotiationResult(
            usbAudioDeviceFound = true,
            deviceProductName = usbDevice.productName?.toString() ?: "USB Audio Device",
            requestedSampleRateHz = sampleRateHz,
            requestedEncoding = targetEncoding,
            bitPerfectMixerSupportedByRom = false,
            bitPerfectMixerActivated = false,
            fallbackReason = "Android SDK ${Build.VERSION.SDK_INT} is below API 34 (Android 14); PreferredMixerAttributes unavailable."
        )
    }

    fun buildMatchedAudioTrack(sampleRateHz: Int, encoding: Int): AudioTrack {
        val attrs = activeAudioAttributes ?: AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRateHz)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, encoding)
            .coerceAtLeast(16384)
        return AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun releasePreferredMixerAttributes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val dev = activeUsbDevice
            val attr = activeAudioAttributes
            if (dev != null && attr != null) {
                runCatching { audioManager.clearPreferredMixerAttributes(attr, dev) }
            }
        }
        activeUsbDevice = null
        activeAudioAttributes = null
    }
}
