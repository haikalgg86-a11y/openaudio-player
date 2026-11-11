/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#include "pinned_ring_buffer.h"
#include <jni.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <algorithm>

namespace openaudio::audio {

bool LockFreePinnedRingBuffer::initialize(size_t requestedBytes) {
    release();
    long sysPage = sysconf(_SC_PAGESIZE);
    size_t pageSize = static_cast<size_t>(sysPage > 0 ? sysPage : 16384);
    size_t alignedSize = ((requestedBytes + pageSize - 1) / pageSize) * pageSize;

    void* ptr = nullptr;
    if (posix_memalign(&ptr, pageSize, alignedSize) != 0 || !ptr) {
        return false;
    }
    buffer_ = static_cast<uint8_t*>(ptr);
    std::memset(buffer_, 0, alignedSize);
    capacityBytes_ = alignedSize;
    isMemLocked_ = (mlock(buffer_, capacityBytes_) == 0);
    writePos_.store(0, std::memory_order_relaxed);
    readPos_.store(0, std::memory_order_relaxed);
    return true;
}

void LockFreePinnedRingBuffer::release() {
    if (buffer_) {
        if (isMemLocked_) {
            munlock(buffer_, capacityBytes_);
        }
        free(buffer_);
        buffer_ = nullptr;
        capacityBytes_ = 0;
        isMemLocked_ = false;
    }
}

void LockFreePinnedRingBuffer::clear() {
    writePos_.store(0, std::memory_order_release);
    readPos_.store(0, std::memory_order_release);
}

size_t LockFreePinnedRingBuffer::availableToRead() const {
    const size_t w = writePos_.load(std::memory_order_acquire);
    const size_t r = readPos_.load(std::memory_order_relaxed);
    return w - r;
}

size_t LockFreePinnedRingBuffer::availableToWrite() const {
    if (capacityBytes_ == 0) return 0;
    return capacityBytes_ - availableToRead();
}

size_t LockFreePinnedRingBuffer::write(const uint8_t* src, size_t bytes) {
    if (!buffer_ || !src || bytes == 0) return 0;
    const size_t writable = std::min(bytes, availableToWrite());
    if (writable == 0) return 0;

    const size_t w = writePos_.load(std::memory_order_relaxed);
    const size_t offset = w % capacityBytes_;
    const size_t firstChunk = std::min(writable, capacityBytes_ - offset);
    std::memcpy(buffer_ + offset, src, firstChunk);
    if (writable > firstChunk) {
        std::memcpy(buffer_, src + firstChunk, writable - firstChunk);
    }
    writePos_.store(w + writable, std::memory_order_release);
    return writable;
}

size_t LockFreePinnedRingBuffer::read(uint8_t* dst, size_t bytes) {
    if (!buffer_ || !dst || bytes == 0) return 0;
    const size_t readable = std::min(bytes, availableToRead());
    if (readable == 0) return 0;

    const size_t r = readPos_.load(std::memory_order_relaxed);
    const size_t offset = r % capacityBytes_;
    const size_t firstChunk = std::min(readable, capacityBytes_ - offset);
    std::memcpy(dst, buffer_ + offset, firstChunk);
    if (readable > firstChunk) {
        std::memcpy(dst + firstChunk, buffer_, readable - firstChunk);
    }
    readPos_.store(r + readable, std::memory_order_release);
    return readable;
}

void BiquadPeakingFilter64::configurePeaking(
    double sampleRateHz,
    double centerFreqHz,
    double qFactor,
    double gainDb
) {
    if (std::abs(gainDb) < 1e-4 || sampleRateHz <= 0.0 || centerFreqHz <= 0.0) {
        b0_ = 1.0; b1_ = 0.0; b2_ = 0.0; a1_ = 0.0; a2_ = 0.0;
        isUnity_ = true;
        return;
    }
    isUnity_ = false;
    const double A = std::pow(10.0, gainDb / 40.0);
    const double w0 = 2.0 * 3.14159265358979323846 * centerFreqHz / sampleRateHz;
    const double alpha = std::sin(w0) / (2.0 * std::max(0.1, qFactor));
    const double cosw0 = std::cos(w0);

    const double a0 = 1.0 + alpha / A;
    b0_ = (1.0 + alpha * A) / a0;
    b1_ = (-2.0 * cosw0) / a0;
    b2_ = (1.0 - alpha * A) / a0;
    a1_ = (-2.0 * cosw0) / a0;
    a2_ = (1.0 - alpha / A) / a0;
}

void BiquadPeakingFilter64::resetState() {
    z1L_ = 0.0; z2L_ = 0.0;
    z1R_ = 0.0; z2R_ = 0.0;
}

void BiquadPeakingFilter64::processStereoS32Le(
    int32_t* interleavedSamples,
    size_t frameCount,
    bool bitPerfectBypass
) {
    // Hard-bypass guarantee: when Bit-Perfect Mode is active, not a single bit is touched.
    if (bitPerfectBypass || isUnity_ || !interleavedSamples) {
        return;
    }

    constexpr double kMaxInt32 = 2147483647.0;
    constexpr double kMinInt32 = -2147483648.0;

    for (size_t i = 0; i < frameCount; ++i) {
        const double inL = static_cast<double>(interleavedSamples[i * 2]);
        const double outL = b0_ * inL + z1L_;
        z1L_ = b1_ * inL - a1_ * outL + z2L_;
        z2L_ = b2_ * inL - a2_ * outL;

        const double inR = static_cast<double>(interleavedSamples[i * 2 + 1]);
        const double outR = b0_ * inR + z1R_;
        z1R_ = b1_ * inR - a1_ * outR + z2R_;
        z2R_ = b2_ * inR - a2_ * outR;

        interleavedSamples[i * 2] = static_cast<int32_t>(std::clamp(std::llrint(outL), static_cast<long long>(kMinInt32), static_cast<long long>(kMaxInt32)));
        interleavedSamples[i * 2 + 1] = static_cast<int32_t>(std::clamp(std::llrint(outR), static_cast<long long>(kMinInt32), static_cast<long long>(kMaxInt32)));
    }
}

} // namespace openaudio::audio

extern "C" JNIEXPORT jint JNICALL
Java_org_openaudio_ndk_audio_NativeAudioBufferBridge_nativeGetRuntimePageSize(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    long sysPage = sysconf(_SC_PAGESIZE);
    return static_cast<jint>(sysPage > 0 ? sysPage : 16384);
}
