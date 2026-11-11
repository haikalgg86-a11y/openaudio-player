/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#pragma once

#include <cstdint>
#include <cstddef>
#include <atomic>
#include <vector>
#include <linux/usbdevice_fs.h>
#include "uac_feedback_parser.h"

namespace openaudio::usb {

struct UsbStreamSessionConfig {
    int fd = -1;
    int audioControlInterface = 0;
    int audioStreamingInterface = 1;
    int alternateSetting = 1;
    uint8_t isocOutEndpoint = 0x01;
    uint8_t feedbackInEndpoint = 0x00;
    bool isUac2 = true;
    bool isHighSpeed = true;
    uint8_t clockSourceId = 1;
    uint32_t sampleRateHz = 96000;
    uint32_t channels = 2;
    uint32_t subslotBytes = 4; // 2 (16-bit), 3 (24-bit packed), or 4 (24/32-bit)
    uint32_t maxPacketSizeBytes = 1024;
    uint32_t numUrbs = 32;
    uint32_t packetsPerUrb = 8;
};

struct UsbEngineLiveStats {
    uint64_t submittedUrbs = 0;
    uint64_t completedUrbs = 0;
    uint64_t urbErrors = 0;
    uint64_t underruns = 0;
    uint32_t negotiatedSampleRateHz = 0;
    uint32_t measuredFeedbackQ16 = 0;
    bool interfaceClaimed = false;
    bool clockVerifiedByGetCur = false;
    int lastErrno = 0;
};

class DirectUsbIsochronousEngine {
public:
    DirectUsbIsochronousEngine() = default;
    ~DirectUsbIsochronousEngine() { stopAndRelease(); }

    /**
     * Executes protocol-matched UAC1/UAC2 rate transition and interface setup:
     * 1. Claims AudioControl & AudioStreaming interfaces via USBDEVFS_DISCONNECT_CLAIM
     * 2. Resets AudioStreaming interface to AltSetting 0 (Zero Bandwidth)
     * 3. Configures Sample Rate via UAC1 Endpoint Control or UAC2 Clock Source Control (SET_CUR + GET_CUR readback)
     * 4. Selects active AudioStreaming AltSetting via USBDEVFS_SETINTERFACE
     */
    int configureHardwareAndClock(const UsbStreamSessionConfig& cfg);

    /**
     * Configures UAC1 / UAC2 Hardware Feature Unit Volume (in 1/256 dB steps; 0x0000 = 0.0 dBFS Unity Gain).
     */
    int setHardwareFeatureUnitVolume(int fd, int acInterface, uint8_t featureUnitId, int16_t volumeSteps256Db, bool isUac2);

    /**
     * Submits a single Isochronous URB block via ioctl(fd, USBDEVFS_SUBMITURB) and reaps via USBDEVFS_REAPURB.
     * Used by the real-time streaming loop.
     */
    int submitAndReapSingleTestUrb(const uint8_t* pcmPayload, size_t payloadLength);

    void stopAndRelease();

    UsbEngineLiveStats getLiveStats() const { return stats_; }

private:
    UsbStreamSessionConfig config_{};
    UsbEngineLiveStats stats_{};
    UacFeedbackParser feedbackParser_{};
};

} // namespace openaudio::usb
