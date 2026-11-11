/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.data.repository

import org.openaudio.audio.engine.DualAudioEngineCoordinator
import org.openaudio.core.preferences.OpenAudioPreferencesDataSource
import org.openaudio.usb.detection.UsbDacDetector
import org.openaudio.usb.transfer.UsbInterfaceClaimCoordinator

class OpenAudioRepository(
    val preferences: OpenAudioPreferencesDataSource = OpenAudioPreferencesDataSource(),
    val usbDetector: UsbDacDetector = UsbDacDetector(),
    val audioCoordinator: DualAudioEngineCoordinator = DualAudioEngineCoordinator()
) {
    val feasibilityMatrix = UsbInterfaceClaimCoordinator.getPhase11FeasibilityMatrix()
}
