/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
#include <jni.h>
#include "lossless_pcm_decoder.h"

namespace openaudio::decoder {

uint64_t computePcmFrameHash64(const uint8_t* pcmBytes, size_t length, uint64_t seed) {
    uint64_t hash = seed ^ 0xcbf29ce484222325ULL;
    for (size_t i = 0; i < length; ++i) {
        hash ^= static_cast<uint64_t>(pcmBytes[i]);
        hash *= 0x100000001b3ULL;
    }
    return hash;
}

} // namespace openaudio::decoder

extern "C" JNIEXPORT jlong JNICALL
Java_org_openaudio_native_decoder_NativeDecoderBridge_nativeComputePcmHash64(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray pcmData,
    jint length,
    jlong seed
) {
    if (!pcmData || length <= 0) return seed;
    jbyte* rawPtr = env->GetByteArrayElements(pcmData, nullptr);
    if (!rawPtr) return seed;
    uint64_t result = openaudio::decoder::computePcmFrameHash64(
        reinterpret_cast<const uint8_t*>(rawPtr),
        static_cast<size_t>(length),
        static_cast<uint64_t>(seed)
    );
    env->ReleaseByteArrayElements(pcmData, rawPtr, JNI_ABORT);
    return static_cast<jlong>(result);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_openaudio_native_decoder_NativeDecoderBridge_nativeProbeHeader(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray headerBytes,
    jint length
) {
    if (!headerBytes || length < 44) return nullptr;
    jbyte* rawPtr = env->GetByteArrayElements(headerBytes, nullptr);
    if (!rawPtr) return nullptr;

    auto meta = openaudio::decoder::LosslessPcmDecoder::probeHeader(
        reinterpret_cast<const uint8_t*>(rawPtr),
        static_cast<size_t>(length)
    );
    env->ReleaseByteArrayElements(headerBytes, rawPtr, JNI_ABORT);

    jint outValues[6] = {
        meta.valid ? 1 : 0,
        static_cast<jint>(meta.format),
        static_cast<jint>(meta.sampleRateHz),
        static_cast<jint>(meta.channels),
        static_cast<jint>(meta.bitDepth),
        static_cast<jint>(meta.totalFrames & 0x7FFFFFFF)
    };
    jintArray arr = env->NewIntArray(6);
    if (arr) {
        env->SetIntArrayRegion(arr, 0, 6, outValues);
    }
    return arr;
}
