/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#include <jni.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <algorithm>
#include <linux/usbdevice_fs.h>
#include "usbdevfs_isoc_engine.h"

namespace openaudio::usb {

int DirectUsbIsochronousEngine::configureHardwareAndClock(const UsbStreamSessionConfig& cfg) {
    config_ = cfg;
    stats_ = UsbEngineLiveStats{};
    if (cfg.fd < 0) {
        stats_.lastErrno = EINVAL;
        return -EINVAL;
    }

    // Step 1: Atomically disconnect kernel snd-usb-audio and claim AudioControl & AudioStreaming interfaces
    const int interfacesToClaim[2] = { cfg.audioControlInterface, cfg.audioStreamingInterface };
    for (int intf : interfacesToClaim) {
        struct usbdevfs_disconnect_claim dc{};
        dc.interface = static_cast<unsigned int>(intf);
        dc.flags = USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER;
        std::strncpy(dc.driver, "usbfs", USBDEVFS_MAXDRIVERNAME);
        if (ioctl(cfg.fd, USBDEVFS_DISCONNECT_CLAIM, &dc) < 0 && errno != EBUSY) {
            // Fallback to standard USBDEVFS_CLAIMINTERFACE if already unbound
            unsigned int uIntf = static_cast<unsigned int>(intf);
            if (ioctl(cfg.fd, USBDEVFS_CLAIMINTERFACE, &uIntf) < 0) {
                stats_.lastErrno = errno;
                return -errno;
            }
        }
    }
    stats_.interfaceClaimed = true;

    // Step 2: Set AudioStreaming Interface to Alternate Setting 0 (Zero-Bandwidth before clock change)
    struct usbdevfs_setinterface setAltZero{};
    setAltZero.interface = static_cast<unsigned int>(cfg.audioStreamingInterface);
    setAltZero.altsetting = 0;
    ioctl(cfg.fd, USBDEVFS_SETINTERFACE, &setAltZero);

    // Step 3: Configure Sample Rate via UAC2 Clock Source (0x21) or UAC1 Endpoint Control (0x22)
    if (cfg.isUac2) {
        uint8_t ratePayload[4] = {
            static_cast<uint8_t>(cfg.sampleRateHz & 0xFF),
            static_cast<uint8_t>((cfg.sampleRateHz >> 8) & 0xFF),
            static_cast<uint8_t>((cfg.sampleRateHz >> 16) & 0xFF),
            static_cast<uint8_t>((cfg.sampleRateHz >> 24) & 0xFF)
        };
        struct usbdevfs_ctrltransfer setCtrl{};
        setCtrl.bRequestType = 0x21; // Host-to-Device | Class | Interface
        setCtrl.bRequest = 0x01;     // CUR
        setCtrl.wValue = 0x0100;     // CS_SAMPLING_FREQ_CONTROL
        setCtrl.wIndex = static_cast<__u16>((cfg.clockSourceId << 8) | (cfg.audioControlInterface & 0xFF));
        setCtrl.wLength = 4;
        setCtrl.timeout = 1000;
        setCtrl.data = ratePayload;

        if (ioctl(cfg.fd, USBDEVFS_CONTROL, &setCtrl) >= 0) {
            uint8_t readback[4] = {0, 0, 0, 0};
            struct usbdevfs_ctrltransfer getCtrl = setCtrl;
            getCtrl.bRequestType = 0xA1; // Device-to-Host | Class | Interface
            getCtrl.data = readback;
            if (ioctl(cfg.fd, USBDEVFS_CONTROL, &getCtrl) == 4) {
                uint32_t actualRate = static_cast<uint32_t>(readback[0]) |
                                      (static_cast<uint32_t>(readback[1]) << 8) |
                                      (static_cast<uint32_t>(readback[2]) << 16) |
                                      (static_cast<uint32_t>(readback[3]) << 24);
                stats_.negotiatedSampleRateHz = actualRate;
                stats_.clockVerifiedByGetCur = (actualRate == cfg.sampleRateHz);
            }
        }
    } else {
        uint8_t ratePayload[3] = {
            static_cast<uint8_t>(cfg.sampleRateHz & 0xFF),
            static_cast<uint8_t>((cfg.sampleRateHz >> 8) & 0xFF),
            static_cast<uint8_t>((cfg.sampleRateHz >> 16) & 0xFF)
        };
        struct usbdevfs_ctrltransfer setCtrl{};
        setCtrl.bRequestType = 0x22; // Host-to-Device | Class | Endpoint
        setCtrl.bRequest = 0x01;     // UAC_SET_CUR
        setCtrl.wValue = 0x0100;     // SAMPLING_FREQ_CONTROL
        setCtrl.wIndex = static_cast<__u16>(cfg.isocOutEndpoint);
        setCtrl.wLength = 3;
        setCtrl.timeout = 1000;
        setCtrl.data = ratePayload;

        if (ioctl(cfg.fd, USBDEVFS_CONTROL, &setCtrl) >= 0) {
            uint8_t readback[3] = {0, 0, 0};
            struct usbdevfs_ctrltransfer getCtrl = setCtrl;
            getCtrl.bRequestType = 0xA2; // Device-to-Host | Class | Endpoint
            getCtrl.bRequest = 0x81;     // UAC_GET_CUR
            getCtrl.data = readback;
            if (ioctl(cfg.fd, USBDEVFS_CONTROL, &getCtrl) == 3) {
                uint32_t actualRate = static_cast<uint32_t>(readback[0]) |
                                      (static_cast<uint32_t>(readback[1]) << 8) |
                                      (static_cast<uint32_t>(readback[2]) << 16);
                stats_.negotiatedSampleRateHz = actualRate;
                stats_.clockVerifiedByGetCur = (actualRate == cfg.sampleRateHz);
            }
        }
    }

    // Step 4: Activate AudioStreaming Alternate Setting via USBDEVFS_SETINTERFACE
    struct usbdevfs_setinterface setAltActive{};
    setAltActive.interface = static_cast<unsigned int>(cfg.audioStreamingInterface);
    setAltActive.altsetting = static_cast<unsigned int>(cfg.alternateSetting);
    if (ioctl(cfg.fd, USBDEVFS_SETINTERFACE, &setAltActive) < 0) {
        stats_.lastErrno = errno;
        return -errno;
    }

    feedbackParser_.configure(cfg.sampleRateHz, cfg.isHighSpeed, 0);
    stats_.measuredFeedbackQ16 = feedbackParser_.getMeasuredQ16();
    return 0;
}

int DirectUsbIsochronousEngine::setHardwareFeatureUnitVolume(
    int fd,
    int acInterface,
    uint8_t featureUnitId,
    int16_t volumeSteps256Db,
    bool isUac2
) {
    if (fd < 0) return -EINVAL;
    uint8_t volPayload[2] = {
        static_cast<uint8_t>(volumeSteps256Db & 0xFF),
        static_cast<uint8_t>((volumeSteps256Db >> 8) & 0xFF)
    };
    struct usbdevfs_ctrltransfer ctrl{};
    ctrl.bRequestType = 0x21;
    ctrl.bRequest = 0x01; // SET_CUR
    ctrl.wValue = 0x0200; // FU_VOLUME_CONTROL (Master Channel 0)
    ctrl.wIndex = static_cast<__u16>((featureUnitId << 8) | (acInterface & 0xFF));
    ctrl.wLength = 2;
    ctrl.timeout = 500;
    ctrl.data = volPayload;
    (void)isUac2;
    return ioctl(fd, USBDEVFS_CONTROL, &ctrl) < 0 ? -errno : 0;
}

int DirectUsbIsochronousEngine::submitAndReapSingleTestUrb(
    const uint8_t* pcmPayload,
    size_t payloadLength
) {
    if (config_.fd < 0 || !pcmPayload || payloadLength == 0) {
        return -EINVAL;
    }
    const uint32_t numPackets = std::max<uint32_t>(1u, config_.packetsPerUrb);
    const size_t urbStructSize = sizeof(struct usbdevfs_urb) +
                                 numPackets * sizeof(struct usbdevfs_iso_packet_desc);
    std::vector<uint8_t> urbStorage(urbStructSize, 0);
    std::vector<uint8_t> dmaBuffer(payloadLength, 0);
    std::memcpy(dmaBuffer.data(), pcmPayload, payloadLength);

    auto* urb = reinterpret_cast<struct usbdevfs_urb*>(urbStorage.data());
    urb->type = USBDEVFS_URB_TYPE_ISO;
    urb->endpoint = config_.isocOutEndpoint;
    urb->flags = USBDEVFS_URB_ISO_ASAP;
    urb->buffer = dmaBuffer.data();
    urb->buffer_length = static_cast<int>(payloadLength);
    urb->number_of_packets = static_cast<int>(numPackets);

    const uint32_t basePacketBytes = static_cast<uint32_t>(payloadLength / numPackets);
    uint32_t remaining = static_cast<uint32_t>(payloadLength);
    for (uint32_t i = 0; i < numPackets; ++i) {
        const uint32_t pktLen = (i + 1 == numPackets) ? remaining : basePacketBytes;
        urb->iso_frame_desc[i].length = pktLen;
        urb->iso_frame_desc[i].actual_length = 0;
        urb->iso_frame_desc[i].status = 0;
        remaining -= pktLen;
    }

    if (ioctl(config_.fd, USBDEVFS_SUBMITURB, urb) < 0) {
        stats_.lastErrno = errno;
        stats_.urbErrors++;
        return -errno;
    }
    stats_.submittedUrbs++;

    void* reapedPtr = nullptr;
    if (ioctl(config_.fd, USBDEVFS_REAPURB, &reapedPtr) < 0) {
        stats_.lastErrno = errno;
        stats_.urbErrors++;
        return -errno;
    }

    if (urb->status == 0 && urb->error_count == 0) {
        stats_.completedUrbs++;
        return 0;
    }
    stats_.urbErrors++;
    return urb->status != 0 ? urb->status : -EIO;
}

void DirectUsbIsochronousEngine::stopAndRelease() {
    if (config_.fd >= 0 && stats_.interfaceClaimed) {
        struct usbdevfs_setinterface setAltZero{};
        setAltZero.interface = static_cast<unsigned int>(config_.audioStreamingInterface);
        setAltZero.altsetting = 0;
        ioctl(config_.fd, USBDEVFS_SETINTERFACE, &setAltZero);

        unsigned int asIntf = static_cast<unsigned int>(config_.audioStreamingInterface);
        unsigned int acIntf = static_cast<unsigned int>(config_.audioControlInterface);
        ioctl(config_.fd, USBDEVFS_RELEASEINTERFACE, &asIntf);
        ioctl(config_.fd, USBDEVFS_RELEASEINTERFACE, &acIntf);
        stats_.interfaceClaimed = false;
    }
}

} // namespace openaudio::usb

namespace {
openaudio::usb::UacFeedbackParser gFeedbackParser;
openaudio::usb::DirectUsbIsochronousEngine gDirectUsbEngine;
} // namespace

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeGetSystemPageSize(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    long pageSize = sysconf(_SC_PAGESIZE);
    return static_cast<jint>(pageSize > 0 ? pageSize : 4096);
}

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeQueryUsbDevFsCapabilities(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint fd
) {
    if (fd < 0) return -EINVAL;
    __u32 capabilities = 0;
    int rc = ioctl(fd, USBDEVFS_GET_CAPABILITIES, &capabilities);
    if (rc < 0) {
        return -errno;
    }
    return static_cast<jint>(capabilities);
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeGetBoundKernelDriver(
    JNIEnv* env,
    jobject /* thiz */,
    jint fd,
    jint interfaceNumber
) {
    if (fd < 0) return env->NewStringUTF("INVALID_FD");
    struct usbdevfs_getdriver getdrv{};
    getdrv.interface = static_cast<unsigned int>(interfaceNumber);
    int rc = ioctl(fd, USBDEVFS_GETDRIVER, &getdrv);
    if (rc < 0) {
        if (errno == ENODATA) {
            return env->NewStringUTF("NONE");
        }
        char errBuf[64];
        std::snprintf(errBuf, sizeof(errBuf), "ERRNO_%d", errno);
        return env->NewStringUTF(errBuf);
    }
    getdrv.driver[USBDEVFS_MAXDRIVERNAME] = 0;
    return env->NewStringUTF(getdrv.driver);
}

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeDisconnectClaimInterface(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint fd,
    jint interfaceNumber
) {
    if (fd < 0) return -EINVAL;
    struct usbdevfs_disconnect_claim dc{};
    dc.interface = static_cast<unsigned int>(interfaceNumber);
    dc.flags = USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER;
    std::strncpy(dc.driver, "usbfs", USBDEVFS_MAXDRIVERNAME);
    int rc = ioctl(fd, USBDEVFS_DISCONNECT_CLAIM, &dc);
    if (rc < 0) {
        return -errno;
    }
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeConfigureFeedbackParser(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint sampleRateHz,
    jboolean isHighSpeed,
    jint dataIntervalLog2
) {
    gFeedbackParser.configure(
        static_cast<uint32_t>(sampleRateHz),
        isHighSpeed != 0,
        static_cast<uint8_t>(dataIntervalLog2)
    );
    return static_cast<jint>(gFeedbackParser.getNominalQ16());
}

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_usb_NativeUsbDevFsBridge_nativeConfigureDirectUsbStream(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint fd,
    jint acInterface,
    jint asInterface,
    jint altSetting,
    jint isocOutEp,
    jboolean isUac2,
    jboolean isHighSpeed,
    jint clockSourceId,
    jint sampleRateHz,
    jint channels,
    jint subslotBytes
) {
    openaudio::usb::UsbStreamSessionConfig cfg{};
    cfg.fd = fd;
    cfg.audioControlInterface = acInterface;
    cfg.audioStreamingInterface = asInterface;
    cfg.alternateSetting = altSetting;
    cfg.isocOutEndpoint = static_cast<uint8_t>(isocOutEp & 0xFF);
    cfg.isUac2 = (isUac2 != 0);
    cfg.isHighSpeed = (isHighSpeed != 0);
    cfg.clockSourceId = static_cast<uint8_t>(clockSourceId & 0xFF);
    cfg.sampleRateHz = static_cast<uint32_t>(sampleRateHz);
    cfg.channels = static_cast<uint32_t>(channels);
    cfg.subslotBytes = static_cast<uint32_t>(subslotBytes);
    return gDirectUsbEngine.configureHardwareAndClock(cfg);
}
