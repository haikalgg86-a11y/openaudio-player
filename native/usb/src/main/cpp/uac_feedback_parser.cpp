/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#include "uac_feedback_parser.h"
#include <algorithm>

namespace openaudio::usb {

void UacFeedbackParser::configure(uint32_t sampleRateHz, bool isHighSpeed, uint8_t dataIntervalLog2) {
    const uint32_t basePacketsPerSec = isHighSpeed ? 8000u : 1000u;
    const uint32_t packetsPerSec = std::max<uint32_t>(1u, basePacketsPerSec >> dataIntervalLog2);
    const uint64_t scaled = (static_cast<uint64_t>(sampleRateHz) << 16) / packetsPerSec;
    nominalFreqQ16_ = static_cast<uint32_t>(scaled);
    measuredFreqQ16_ = nominalFreqQ16_;
    phaseAccumulatorQ16_ = 0;
    detectedShift_ = kShiftUninitialized;
}

void UacFeedbackParser::reset() {
    measuredFreqQ16_ = nominalFreqQ16_;
    phaseAccumulatorQ16_ = 0;
    detectedShift_ = kShiftUninitialized;
}

uint32_t UacFeedbackParser::parseFeedbackPacket(const uint8_t* data, size_t actualLength) {
    if (!data || (actualLength != 3 && actualLength != 4) || nominalFreqQ16_ == 0) {
        return measuredFreqQ16_;
    }

    uint32_t raw = static_cast<uint32_t>(data[0]) |
                   (static_cast<uint32_t>(data[1]) << 8) |
                   (static_cast<uint32_t>(data[2]) << 16);
    if (actualLength == 4) {
        raw |= (static_cast<uint32_t>(data[3]) << 24);
        raw &= 0x0FFFFFFFu; // Mask upper status nibble if present on certain USB bridges
    } else {
        raw &= 0x00FFFFFFu;
    }

    if (raw == 0) {
        return measuredFreqQ16_;
    }

    const uint32_t lowerBound = (nominalFreqQ16_ * 3u) / 4u; // -25% discovery window
    const uint32_t upperBound = (nominalFreqQ16_ * 5u) / 4u; // +25% discovery window

    if (detectedShift_ == kShiftUninitialized) {
        for (int32_t shift = -4; shift <= 6; ++shift) {
            const uint32_t candidate = (shift >= 0)
                ? (raw << shift)
                : (raw >> (-shift));
            if (candidate >= lowerBound && candidate <= upperBound) {
                detectedShift_ = shift;
                break;
            }
        }
        if (detectedShift_ == kShiftUninitialized) {
            return measuredFreqQ16_;
        }
    }

    const uint32_t shifted = (detectedShift_ >= 0)
        ? (raw << detectedShift_)
        : (raw >> (-detectedShift_));

    // Clamp against transient glitches (+/- 12.5% safety envelope around nominal rate)
    const uint32_t minSafe = nominalFreqQ16_ - (nominalFreqQ16_ >> 3);
    const uint32_t maxSafe = nominalFreqQ16_ + (nominalFreqQ16_ >> 3);
    if (shifted < minSafe || shifted > maxSafe) {
        return measuredFreqQ16_;
    }

    // Exponential moving average smoothing (1/4 weight) to suppress SOF jitter
    measuredFreqQ16_ = static_cast<uint32_t>(
        static_cast<int64_t>(measuredFreqQ16_) +
        ((static_cast<int64_t>(shifted) - static_cast<int64_t>(measuredFreqQ16_)) >> 2)
    );
    return measuredFreqQ16_;
}

uint32_t UacFeedbackParser::nextPacketSampleCount(uint32_t maxSamplesPerPacket) {
    phaseAccumulatorQ16_ = (phaseAccumulatorQ16_ & 0xFFFFu) + measuredFreqQ16_;
    const uint32_t samples = phaseAccumulatorQ16_ >> 16;
    return std::min<uint32_t>(samples, maxSamplesPerPacket);
}

} // namespace openaudio::usb
