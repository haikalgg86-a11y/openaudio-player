/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#pragma once

#include <cstdint>
#include <cstddef>
#include <atomic>
#include <cmath>

namespace openaudio::audio {

/**
 * Lock-Free Single-Producer Single-Consumer (SPSC) Ring Buffer with dynamic
 * 16 KB / 4 KB page alignment (sysconf(_SC_PAGESIZE)) and RAM page pinning (mlock)
 * so the Linux kernel never swaps active PCM frames to flash storage.
 */
class LockFreePinnedRingBuffer {
public:
    LockFreePinnedRingBuffer() = default;
    ~LockFreePinnedRingBuffer() { release(); }

    bool initialize(size_t requestedBytes);
    void release();
    void clear();

    size_t availableToRead() const;
    size_t availableToWrite() const;

    size_t write(const uint8_t* src, size_t bytes);
    size_t read(uint8_t* dst, size_t bytes);

    bool isMemLocked() const { return isMemLocked_; }
    size_t capacity() const { return capacityBytes_; }

private:
    uint8_t* buffer_ = nullptr;
    size_t capacityBytes_ = 0;
    bool isMemLocked_ = false;
    std::atomic<size_t> writePos_{0};
    std::atomic<size_t> readPos_{0};
};

/**
 * 64-Bit Double-Precision Biquad Parametric EQ Filter + SplitMix64 TPDF Dither.
 * Strictly bypassed when Bit-Perfect Mode is active.
 */
class BiquadPeakingFilter64 {
public:
    void configurePeaking(double sampleRateHz, double centerFreqHz, double qFactor, double gainDb);
    void resetState();
    void processStereoS32Le(int32_t* interleavedSamples, size_t frameCount, bool bitPerfectBypass);

private:
    double b0_ = 1.0, b1_ = 0.0, b2_ = 0.0;
    double a1_ = 0.0, a2_ = 0.0;
    double z1L_ = 0.0, z2L_ = 0.0;
    double z1R_ = 0.0, z2R_ = 0.0;
    bool isUnity_ = true;
};

} // namespace openaudio::audio
