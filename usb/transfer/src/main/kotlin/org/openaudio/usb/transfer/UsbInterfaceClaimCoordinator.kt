/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.usb.transfer

import org.openaudio.core.model.FeasibilityCategory
import org.openaudio.native.usb.NativeUsbDevFsBridge

data class UsbFeasibilityAuditEntry(
    val capabilityName: String,
    val mechanism: String,
    val category: FeasibilityCategory,
    val runtimeNotes: String
)

/**
 * Coordinates the Dual-Stage Interface Acquisition protocol defined in Phase 1.1:
 * Stage 1: Android framework UsbDeviceConnection.claimInterface(intf, true)
 * Stage 2: Native ioctl(fd, USBDEVFS_DISCONNECT_CLAIM) fallback with explicit errno reporting.
 */
object UsbInterfaceClaimCoordinator {
    fun querySystemPageSize(): Int {
        return if (NativeUsbDevFsBridge.isNativeReady()) {
            NativeUsbDevFsBridge.nativeGetSystemPageSize()
        } else {
            16384
        }
    }

    fun getPhase11FeasibilityMatrix(): List<UsbFeasibilityAuditEntry> = listOf(
        UsbFeasibilityAuditEntry(
            capabilityName = "USB Device Discovery & Permission Grant",
            mechanism = "UsbManager.getDeviceList() + requestPermission()",
            category = FeasibilityCategory.CONFIRMED_BY_OFFICIAL_DOCS,
            runtimeNotes = "Standard Android USB Host API available since API 12 without root."
        ),
        UsbFeasibilityAuditEntry(
            capabilityName = "Native File Descriptor Passing",
            mechanism = "UsbDeviceConnection.getFileDescriptor()",
            category = FeasibilityCategory.CONFIRMED_BY_OFFICIAL_DOCS,
            runtimeNotes = "Official SDK method returning open kernel usbfs fd for JNI/NDK use."
        ),
        UsbFeasibilityAuditEntry(
            capabilityName = "Direct C++ open('/dev/bus/usb/...')",
            mechanism = "POSIX open() syscall from untrusted_app",
            category = FeasibilityCategory.NOT_SUPPORTED,
            runtimeNotes = "Blocked by Android SELinux (app.te). Must always inherit fd from UsbDeviceConnection."
        ),
        UsbFeasibilityAuditEntry(
            capabilityName = "Unbinding Kernel snd-usb-audio Driver",
            mechanism = "claimInterface(intf, true) / USBDEVFS_DISCONNECT_CLAIM",
            category = FeasibilityCategory.ROM_DEPENDENT,
            runtimeNotes = "May require 'Disable USB audio routing' in Developer Options if audioserver re-claims ALSA node."
        ),
        UsbFeasibilityAuditEntry(
            capabilityName = "Isochronous URB Pipeline Submission",
            mechanism = "ioctl(fd, USBDEVFS_SUBMITURB / USBDEVFS_REAPURB)",
            category = FeasibilityCategory.KERNEL_DEPENDENT,
            runtimeNotes = "Requires kernel xHCI isochronous support, usbfs_memory_mb headroom, and SELinux ioctl allowlist."
        ),
        UsbFeasibilityAuditEntry(
            capabilityName = "Screen-Off Glitch-Free Streaming",
            mechanism = "ForegroundService(mediaPlayback) + PARTIAL_WAKE_LOCK + mlock()",
            category = FeasibilityCategory.REQUIRES_DEVICE_TESTING,
            runtimeNotes = "Must be validated on target Android 16 Custom ROM physical hardware over 30+ min playback."
        )
    )
}
