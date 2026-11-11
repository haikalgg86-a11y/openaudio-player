#!/usr/bin/env bash
# ==============================================================================
# OpenAudio Player — One-Click APK Builder & Physical Device Installer (Linux/macOS)
# Target: Android 16 (API 36), arm64-v8a (16 KB Page Size Aligned)
# ==============================================================================
set -euo pipefail

echo "============================================================"
echo " OpenAudio Player — Android 16 (API 36) APK Build Script"
echo "============================================================"

if command -v ./gradlew &> /dev/null; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "[ERROR] Neither ./gradlew nor gradle was found in PATH."
    echo "Please open this folder in Android Studio or install Gradle 8.9+."
    exit 1
fi

echo "[1/3] Compiling 25 Kotlin Modules + 3 Native C++17 NDK Libraries (16 KB ELF Aligned)..."
$GRADLE_CMD assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
echo "[2/3] APK successfully generated at: $APK_PATH"

if command -v adb &> /dev/null; then
    if adb get-state 1>/dev/null 2>&1; then
        echo "[3/3] Physical Android device detected via ADB! Installing $APK_PATH..."
        adb install -r "$APK_PATH"
        echo "[SUCCESS] OpenAudio Player installed on device."
    else
        echo "[3/3] No Android device currently connected via ADB. Copy $APK_PATH to your phone to install."
    fi
else
    echo "[3/3] ADB not in PATH. Copy $APK_PATH to your phone to install."
fi
