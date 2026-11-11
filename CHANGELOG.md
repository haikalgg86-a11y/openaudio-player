# Changelog

Seluruh perubahan penting pada proyek **OpenAudio Player** didokumentasikan dalam file ini.
Format mengacu pada [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0-alpha01] - 2026-11-11 (PHASE 2 — FOUNDATION)

### Ditambahkan
- Inisialisasi struktur proyek multi-modul Gradle Kotlin DSL (25 modul: `app`, `core:*`, `audio:*`, `usb:*`, `player:*`, `data:*`, `feature:*`, `native:*`).
- Target konfigurasi Android 16 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, ABI utama `arm64-v8a`).
- Konfigurasi Android NDK & CMake C++17 dengan dukungan penuh **16 KB ELF Page Size Alignment** (`-Wl,-z,max-page-size=16384`) dan deteksi ukuran halaman memori dinamis (`sysconf(_SC_PAGESIZE)`).
- Implementasi model domain inti (`core:model`) mencakup sistem verifikasi Bit-Perfect 5 tingkat (`PCM_PIPELINE_VERIFIED`, `USB_URB_SUBMISSION_VERIFIED`, `USB_TRANSFER_COMPLETION_VERIFIED`, `ANDROID_HAL_BIT_PERFECT_REPORTED`, `HARDWARE_OUTPUT_UNVERIFIED`) dan model feasibility akses `usbdevfs`.
- Implementasi penyimpanan pengaturan aplikasi berbasis Jetpack DataStore (`core:preferences`).
- Implementasi jembatan JNI dan pemeriksa kapabilitas kernel `usbdevfs` (`USBDEVFS_GET_CAPABILITIES`, `USBDEVFS_GETDRIVER`) di `native:usb`, `native:audio`, dan `native:decoder`.
- Implementasi Tema **Dark AMOLED Audiophile Material 3** dan kerangka navigasi Jetpack Compose untuk 10 halaman utama (*Home, Music Library, Folder Browser, Now Playing, USB DAC Manager, Audio Engine, Equalizer, Audio Settings, Device Information, About OpenAudio*).
- Dokumen tata kelola open-source Apache-2.0 (`LICENSE`, `README.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `THIRD_PARTY_NOTICES.md`, `SECURITY.md`, `CHANGELOG.md`).
