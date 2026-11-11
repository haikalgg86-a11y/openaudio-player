/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.native.usb

object NativeUsbDevFsBridge {
    private var isLibraryLoaded = false

    init {
        isLibraryLoaded = runCatching {
            System.loadLibrary("openaudio_usb")
            true
        }.getOrDefault(false)
    }

    fun isNativeReady(): Boolean = isLibraryLoaded

    external fun nativeGetSystemPageSize(): Int
    external fun nativeQueryUsbDevFsCapabilities(fd: Int): Int
    external fun nativeGetBoundKernelDriver(fd: Int, interfaceNumber: Int): String
    external fun nativeDisconnectClaimInterface(fd: Int, interfaceNumber: Int): Int
    external fun nativeConfigureFeedbackParser(sampleRateHz: Int, isHighSpeed: Boolean, dataIntervalLog2: Int): Int
    external fun nativeConfigureDirectUsbStream(
        fd: Int,
        acInterface: Int,
        asInterface: Int,
        altSetting: Int,
        isocOutEp: Int,
        isUac2: Boolean,
        isHighSpeed: Boolean,
        clockSourceId: Int,
        sampleRateHz: Int,
        channels: Int,
        subslotBytes: Int
    ): Int
}
