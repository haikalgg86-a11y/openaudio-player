@echo off
REM ==============================================================================
REM OpenAudio Player — One-Click APK Builder & Installer (Windows)
REM Target: Android 16 (API 36), arm64-v8a (16 KB Page Size Aligned)
REM ==============================================================================
echo ============================================================
echo  OpenAudio Player — Android 16 (API 36) APK Build Script
echo ============================================================

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [1/2] Building Debug APK via Gradle...
    gradle assembleDebug
    echo [2/2] APK generated at: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Please open this project directory in Android Studio and click Run / Build APK(s).
)
pause
