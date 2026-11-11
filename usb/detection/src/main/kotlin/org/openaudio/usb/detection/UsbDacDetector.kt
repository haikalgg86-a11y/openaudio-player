/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.usb.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openaudio.core.model.DacDeviceInfo

/**
 * Monitors USB DAC hotplug events and exposes current connection state.
 * Never fabricates connected hardware when no physical USB DAC is attached.
 */
class UsbDacDetector {
    private val _connectedDac = MutableStateFlow<DacDeviceInfo?>(null)
    val connectedDac: StateFlow<DacDeviceInfo?> = _connectedDac.asStateFlow()

    fun onDacAttached(info: DacDeviceInfo) {
        _connectedDac.value = info
    }

    fun onDacDetached() {
        _connectedDac.value = null
    }
}
