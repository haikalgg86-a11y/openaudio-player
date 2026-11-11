/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#pragma once

#include <cstdint>
#include <cstddef>
#include <vector>
#include <string>

namespace openaudio::decoder {

enum class DetectedFormat : int32_t {
    UNKNOWN = 0,
    WAV_PCM = 1,
    AIFF_PCM = 2,
    FLAC = 3,
    DSF_DSD = 4,
    DFF_DSD = 5,
    OGG_OPUS = 6
};

struct AudioStreamMetadata {
    DetectedFormat format = DetectedFormat::UNKNOWN;
    uint32_t sampleRateHz = 0;
    uint32_t channels = 0;
    uint32_t bitDepth = 0;
    uint64_t totalFrames = 0;
    uint64_t dataOffsetBytes = 0;
    uint64_t dataSizeBytes = 0;
    bool isDsd = false;
    bool valid = false;
};

class LosslessPcmDecoder {
public:
    /**
     * Parses header bytes of WAV, AIFF, FLAC, DSF, or DFF files without floating-point math.
     */
    static AudioStreamMetadata probeHeader(const uint8_t* data, size_t size);

    /**
     * Packs 1-bit DSD stream bytes (DSF/DFF) into 24-bit DoP (DSD over PCM, packed in 32-bit little-endian subslots)
     * using alternating markers 0x05 and 0xFA in the most significant byte.
     * Each stereo DoP frame consumes 2 bytes per channel (16 DSD bits) + 1 marker byte.
     */
    static size_t packDsdToDoP32Le(
        const uint8_t* dsdLeft,
        const uint8_t* dsdRight,
        size_t dsdBytesPerChannel,
        int32_t* outDoP32Interleaved,
        bool& markerToggleState
    );

    /**
     * Converts packed 24-bit little-endian PCM (S24_3LE) to left-justified 32-bit integer (S24_4LE / S32_LE)
     * with zero bit loss (exact reversible bit shift).
     */
    static size_t unpackS24_3LeToS32Le(
        const uint8_t* s24Bytes,
        size_t numSamples,
        int32_t* outS32Samples
    );
};

uint64_t computePcmFrameHash64(const uint8_t* pcmBytes, size_t length, uint64_t seed);

} // namespace openaudio::decoder
