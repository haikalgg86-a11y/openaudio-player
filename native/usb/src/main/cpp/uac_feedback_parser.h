/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#pragma once

#include <cstdint>
#include <cstddef>

namespace openaudio::usb {

/**
 * Adaptive Asynchronous USB Audio Feedback Parser (UAC 1.0 & UAC 2.0).
 * Implements dynamic shift detection inspired by Linux kernel sound/usb/endpoint.c
 * to support standard Full-Speed 10.14 (3-byte), High-Speed 16.16 (4-byte),
 * and non-standard DAC feedback quirks without hardcoding a single format.
 */
class UacFeedbackParser {
public:
    void configure(uint32_t sampleRateHz, bool isHighSpeed, uint8_t dataIntervalLog2);
    void reset();

    /**
     * Parses a raw feedback packet from an isochronous IN feedback URB.
     * Returns the updated Q16.16 samples-per-packet value (freqm).
     */
    uint32_t parseFeedbackPacket(const uint8_t* data, size_t actualLength);

    /**
     * Computes exact sample count for the next outgoing isochronous packet
     * using a Q16.16 fractional phase accumulator.
     */
    uint32_t nextPacketSampleCount(uint32_t maxSamplesPerPacket);

    uint32_t getNominalQ16() const { return nominalFreqQ16_; }
    uint32_t getMeasuredQ16() const { return measuredFreqQ16_; }
    int32_t getDetectedShift() const { return detectedShift_; }

private:
    static constexpr int32_t kShiftUninitialized = -999;
    uint32_t nominalFreqQ16_ = 0;
    uint32_t measuredFreqQ16_ = 0;
    uint32_t phaseAccumulatorQ16_ = 0;
    int32_t detectedShift_ = kShiftUninitialized;
};

} // namespace openaudio::usb
