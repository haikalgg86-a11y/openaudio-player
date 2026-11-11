/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#include "lossless_pcm_decoder.h"
#include <cstring>

namespace openaudio::decoder {

namespace {
uint16_t readLe16(const uint8_t* p) {
    return static_cast<uint16_t>(p[0]) | (static_cast<uint16_t>(p[1]) << 8);
}
uint32_t readLe32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) |
           (static_cast<uint32_t>(p[3]) << 24);
}
uint64_t readLe64(const uint8_t* p) {
    return static_cast<uint64_t>(readLe32(p)) |
           (static_cast<uint64_t>(readLe32(p + 4)) << 32);
}
uint32_t readBe32(const uint8_t* p) {
    return (static_cast<uint32_t>(p[0]) << 24) |
           (static_cast<uint32_t>(p[1]) << 16) |
           (static_cast<uint32_t>(p[2]) << 8) |
           static_cast<uint32_t>(p[3]);
}
} // namespace

AudioStreamMetadata LosslessPcmDecoder::probeHeader(const uint8_t* data, size_t size) {
    AudioStreamMetadata meta{};
    if (!data || size < 42) return meta;

    // 1. Check RIFF/WAV ("RIFF" ... "WAVE")
    if (size >= 44 && std::memcmp(data, "RIFF", 4) == 0 && std::memcmp(data + 8, "WAVE", 4) == 0) {
        size_t offset = 12;
        while (offset + 8 <= size) {
            uint32_t chunkSize = readLe32(data + offset + 4);
            if (std::memcmp(data + offset, "fmt ", 4) == 0 && offset + 8 + 16 <= size) {
                uint16_t audioFormat = readLe16(data + offset + 8);
                uint16_t channels = readLe16(data + offset + 10);
                uint32_t sampleRate = readLe32(data + offset + 12);
                uint16_t bitsPerSample = readLe16(data + offset + 22);
                if (audioFormat == 1 || audioFormat == 0xFFFE) { // PCM or WAVE_FORMAT_EXTENSIBLE
                    meta.format = DetectedFormat::WAV_PCM;
                    meta.channels = channels;
                    meta.sampleRateHz = sampleRate;
                    meta.bitDepth = bitsPerSample;
                }
            } else if (std::memcmp(data + offset, "data", 4) == 0) {
                meta.dataOffsetBytes = offset + 8;
                meta.dataSizeBytes = chunkSize;
                if (meta.channels > 0 && meta.bitDepth > 0) {
                    uint32_t bytesPerFrame = meta.channels * (meta.bitDepth / 8);
                    if (bytesPerFrame > 0) {
                        meta.totalFrames = chunkSize / bytesPerFrame;
                    }
                    meta.valid = true;
                }
                break;
            }
            offset += 8 + ((chunkSize + 1) & ~1u);
        }
        return meta;
    }

    // 2. Check Native FLAC ("fLaC" magic + STREAMINFO metadata block type 0)
    if (std::memcmp(data, "fLaC", 4) == 0 && size >= 42) {
        uint8_t blockHeader = data[4];
        uint8_t blockType = blockHeader & 0x7F;
        if (blockType == 0) { // STREAMINFO block (34 bytes starting at offset 8)
            const uint8_t* si = data + 8;
            uint32_t sampleRate = (static_cast<uint32_t>(si[10]) << 12) |
                                  (static_cast<uint32_t>(si[11]) << 4) |
                                  ((static_cast<uint32_t>(si[12]) >> 4) & 0x0F);
            uint8_t channels = ((si[12] >> 1) & 0x07) + 1;
            uint8_t bitsPerSample = (((si[12] & 0x01) << 4) | ((si[13] >> 4) & 0x0F)) + 1;
            uint64_t totalSamples = (static_cast<uint64_t>(si[13] & 0x0F) << 32) |
                                    static_cast<uint64_t>(readBe32(si + 14));
            meta.format = DetectedFormat::FLAC;
            meta.sampleRateHz = sampleRate;
            meta.channels = channels;
            meta.bitDepth = bitsPerSample;
            meta.totalFrames = totalSamples;
            meta.valid = (sampleRate > 0 && channels > 0);
            return meta;
        }
    }

    // 3. Check Sony DSF ("DSD " magic, 28-byte DSD chunk + 52-byte "fmt " chunk)
    if (std::memcmp(data, "DSD ", 4) == 0 && size >= 80 && std::memcmp(data + 28, "fmt ", 4) == 0) {
        uint32_t channels = readLe32(data + 28 + 24);
        uint32_t samplingFreqHz = readLe32(data + 28 + 28);
        uint32_t bitsPerSample = readLe32(data + 28 + 32);
        uint64_t sampleCount = readLe64(data + 28 + 36);
        meta.format = DetectedFormat::DSF_DSD;
        meta.sampleRateHz = samplingFreqHz;
        meta.channels = channels;
        meta.bitDepth = bitsPerSample; // 1-bit DSD
        meta.totalFrames = sampleCount;
        meta.dataOffsetBytes = 92;
        meta.isDsd = true;
        meta.valid = (samplingFreqHz >= 2822400 && channels > 0);
        return meta;
    }

    // 4. Check Philips DSDIFF / DFF ("FRM8" ... "DSD ")
    if (std::memcmp(data, "FRM8", 4) == 0 && std::memcmp(data + 12, "DSD ", 4) == 0) {
        meta.format = DetectedFormat::DFF_DSD;
        meta.sampleRateHz = 2822400; // Default DSD64 if FS chunk follows
        meta.channels = 2;
        meta.bitDepth = 1;
        meta.isDsd = true;
        meta.valid = true;
        return meta;
    }

    return meta;
}

size_t LosslessPcmDecoder::packDsdToDoP32Le(
    const uint8_t* dsdLeft,
    const uint8_t* dsdRight,
    size_t dsdBytesPerChannel,
    int32_t* outDoP32Interleaved,
    bool& markerToggleState
) {
    if (!dsdLeft || !dsdRight || !outDoP32Interleaved || dsdBytesPerChannel < 2) {
        return 0;
    }
    const size_t dopFrames = dsdBytesPerChannel / 2;
    size_t outIdx = 0;
    for (size_t f = 0; f < dopFrames; ++f) {
        const uint8_t marker = markerToggleState ? 0xFAu : 0x05u;
        markerToggleState = !markerToggleState;

        // Pack 16 bits of DSD + 8-bit DoP marker into upper 24 bits of 32-bit PCM word (S24_4LE)
        const uint32_t wordL = (static_cast<uint32_t>(marker) << 24) |
                               (static_cast<uint32_t>(dsdLeft[f * 2]) << 16) |
                               (static_cast<uint32_t>(dsdLeft[f * 2 + 1]) << 8);
        const uint32_t wordR = (static_cast<uint32_t>(marker) << 24) |
                               (static_cast<uint32_t>(dsdRight[f * 2]) << 16) |
                               (static_cast<uint32_t>(dsdRight[f * 2 + 1]) << 8);

        outDoP32Interleaved[outIdx++] = static_cast<int32_t>(wordL);
        outDoP32Interleaved[outIdx++] = static_cast<int32_t>(wordR);
    }
    return dopFrames;
}

size_t LosslessPcmDecoder::unpackS24_3LeToS32Le(
    const uint8_t* s24Bytes,
    size_t numSamples,
    int32_t* outS32Samples
) {
    if (!s24Bytes || !outS32Samples) return 0;
    for (size_t i = 0; i < numSamples; ++i) {
        const size_t b = i * 3;
        // Place 24-bit sample into the upper 24 bits of 32-bit signed integer (lossless bit-exact shift)
        uint32_t packed = (static_cast<uint32_t>(s24Bytes[b]) << 8) |
                          (static_cast<uint32_t>(s24Bytes[b + 1]) << 16) |
                          (static_cast<uint32_t>(s24Bytes[b + 2]) << 24);
        outS32Samples[i] = static_cast<int32_t>(packed);
    }
    return numSamples;
}

} // namespace openaudio::decoder
