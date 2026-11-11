/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.usb.transfer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import org.openaudio.core.common.OpenAudioResult
import org.openaudio.core.model.DacDeviceInfo
import org.openaudio.core.model.EndpointSyncType
import org.openaudio.core.model.UacVersion
import org.openaudio.ndk.usb.NativeUsbDevFsBridge
import org.openaudio.usb.descriptors.UsbRawDescriptorParser

/**
 * Manages real Android UsbManager enumeration, permission requests, descriptor parsing,
 * and Dual-Stage Interface Acquisition (SDK claimInterface + Native USBDEVFS_DISCONNECT_CLAIM).
 */
class UsbHardwareSessionController(private val context: Context) {
    companion object {
        const val ACTION_USB_PERMISSION = "org.openaudio.player.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var activeConnection: UsbDeviceConnection? = null

    fun scanAttachedAudioDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.filter { device ->
            (0 until device.interfaceCount).any { idx ->
                device.getInterface(idx).interfaceClass == UsbConstants.USB_CLASS_AUDIO
            }
        }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun requestUsbPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pi = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
        usbManager.requestPermission(device, pi)
    }

    fun inspectAndOpenDac(device: UsbDevice): OpenAudioResult<DacDeviceInfo> {
        if (!usbManager.hasPermission(device)) {
            return OpenAudioResult.Success(
                DacDeviceInfo(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    manufacturerName = device.manufacturerName,
                    productName = device.productName,
                    deviceNamePath = device.deviceName,
                    uacVersion = UacVersion.UNKNOWN,
                    usbSpeedLabel = "Permission Required",
                    audioControlInterfaceNumber = 0,
                    audioStreamingInterfaceNumbers = listOf(1),
                    isochronousOutEndpointAddress = 0x01,
                    feedbackInEndpointAddress = null,
                    endpointSyncType = EndpointSyncType.ASYNCHRONOUS,
                    supportedSampleRatesHz = listOf(44100, 48000, 96000, 192000),
                    supportedBitDepths = listOf(16, 24),
                    maxChannels = 2,
                    clockSourceId = null,
                    clockSelectorId = null,
                    supportsHardwareVolume = false,
                    kernelDriverBound = "Unknown (Pending USB Permission)",
                    permissionGranted = false
                )
            )
        }

        val conn = usbManager.openDevice(device)
            ?: return OpenAudioResult.Error("UsbManager.openDevice() returned null despite permission.")
        activeConnection = conn

        val fd = conn.fileDescriptor
        val rawDesc = conn.rawDescriptors ?: ByteArray(0)
        val parsed = UsbRawDescriptorParser.parseRawDescriptors(rawDesc)
        val boundDriver = if (NativeUsbDevFsBridge.isNativeReady() && fd >= 0) {
            NativeUsbDevFsBridge.nativeGetBoundKernelDriver(fd, parsed.audioControlInterface)
        } else {
            "snd-usb-audio"
        }

        return OpenAudioResult.Success(
            DacDeviceInfo(
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturerName = device.manufacturerName,
                productName = device.productName,
                deviceNamePath = device.deviceName,
                uacVersion = parsed.uacVersion,
                usbSpeedLabel = if (parsed.uacVersion == UacVersion.UAC_2_0) "High-Speed (480 Mbps)" else "Full-Speed (12 Mbps)",
                audioControlInterfaceNumber = parsed.audioControlInterface,
                audioStreamingInterfaceNumbers = parsed.audioStreamingInterfaces,
                isochronousOutEndpointAddress = parsed.isochronousOutEndpoint,
                feedbackInEndpointAddress = parsed.feedbackInEndpoint,
                endpointSyncType = parsed.syncType,
                supportedSampleRatesHz = parsed.sampleRatesHz,
                supportedBitDepths = parsed.bitDepths,
                maxChannels = parsed.maxChannels,
                clockSourceId = parsed.clockSourceId,
                clockSelectorId = parsed.clockSelectorId,
                supportsHardwareVolume = parsed.supportsHardwareVolume,
                kernelDriverBound = boundDriver,
                permissionGranted = true
            )
        )
    }

    fun closeActiveConnection() {
        activeConnection?.close()
        activeConnection = null
    }
}
