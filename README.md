# OpenAudio Player

**Open-Source Android Hi-Res Music Player & Direct USB DAC Audio Engine**

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36_(Android_16)-3DDC84.svg)](https://developer.android.com/about/versions/16)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26_(Android_8.0)-3DDC84.svg)](https://developer.android.com)
[![ABI](https://img.shields.io/badge/ABI-arm64--v8a_(16KB_Page_Aligned)-orange.svg)](https://developer.android.com/guide/practices/page-sizes)

OpenAudio Player adalah pemutar musik Hi-Res gratis dan open-source untuk Android (prioritas utama: **Android 16 Custom ROM**, API 36, `arm64-v8a`) yang dibangun sepenuhnya dari kode sumber asli dengan arsitektur bersih dan modular. Proyek ini menyediakan dua jalur output audio independen beserta sistem verifikasi bit-perfect 5 tingkat yang transparan.

---

## Fitur Utama & Filosofi Kejujuran Teknis

1. **Dual Audio Output Architecture:**
   - **Path A — Android Native USB Audio:** Menggunakan `AudioTrack`, `AAudio`, dan API [`AudioManager.setPreferredMixerAttributes()` (`MIXER_BEHAVIOR_BIT_PERFECT`)](https://developer.android.com/media/platform/improve-audio-playback) pada Android 14–16. Mendeteksi secara jujur apakah Audio HAL pada ROM mendukung flag `BIT_PERFECT`.
   - **Path B — Direct USB Audio Engine (C++17 / NDK / Linux `usbdevfs`):** Mengakuisisi file descriptor dari [`UsbDeviceConnection.getFileDescriptor()`](https://developer.android.com/reference/android/hardware/usb/UsbDeviceConnection) dan menjalankan transfer **Isochronous URB** (`USBDEVFS_SUBMITURB` / `USBDEVFS_REAPURB`) secara langsung ke USB DAC (UAC 1.0 & UAC 2.0) dengan parser *asynchronous feedback* adaptif (Q16.16) dan kontrol clock hardware tanpa memerlukan `libusb` maupun akses root (selama kernel dan kebijakan SELinux ROM mengizinkan).
2. **Transparansi Verifikasi Bit-Perfect (5 Tingkat):**
   - **Level A (`PCM_PIPELINE_VERIFIED`):** Hash XXH64 output integer decoder cocok 100% dengan ring buffer keluar (DSP/volume software di-bypass).
   - **Level B (`USB_URB_SUBMISSION_VERIFIED`):** Interface USB diklaim eksklusif, frekuensi clock dikonfirmasi, dan `USBDEVFS_SUBMITURB` menerima payload identik dengan Level A.
   - **Level C (`USB_TRANSFER_COMPLETION_VERIFIED`):** `USBDEVFS_REAPURB` mengembalikan `status == 0`, `error_count == 0`, dan `actual_length == length` pada seluruh paket isochronous.
   - **Level D (`ANDROID_HAL_BIT_PERFECT_REPORTED`):** Framework Android (`AudioManager`) mengonfirmasi atribut `BIT_PERFECT` aktif pada Path A.
   - **Level E (`HARDWARE_OUTPUT_UNVERIFIED`):** Status default untuk klaim perangkat keras di luar jangkauan bus host kecuali divalidasi melalui pengujian *External Digital Loopback*.
3. **Kepatuhan Android 16 (API 36) & 16 KB Page Size:**
   - Seluruh pustaka native C++17 dikompilasi dengan flag `-Wl,-z,max-page-size=16384` dan menggunakan `sysconf(_SC_PAGESIZE)` saat runtime ([Support 16 KB Page Sizes](https://developer.android.com/guide/practices/page-sizes)).

---

## Struktur Modul Proyek (25 Modul)

```text
openaudio-player/
├── app/                        # Entry point aplikasi, navigasi Compose, tema Dark AMOLED Material 3
├── core/
│   ├── common/                 # Coroutine dispatchers, Result wrapper, logger diagnostik
│   ├── model/                  # Domain models (5-Level Verification, DacDeviceInfo, StreamConfig)
│   ├── database/               # Room database schema untuk pustaka musik & profil DAC
│   └── preferences/            # Jetpack DataStore untuk konfigurasi Audio Engine & Bit-Perfect
├── audio/
│   ├── decoder/                # Antarmuka ekstraktor integer PCM & DoP/DSD
│   ├── engine/                 # Dual Output Engine Coordinator (Path A & Path B)
│   ├── dsp/                    # Pipeline DSP 64-bit opsional (otomatis nonaktif saat Bit-Perfect)
│   └── verification/           # Verifikasi hash XXH64 & auditor status 5 tingkat
├── usb/
│   ├── detection/              # UsbManager hotplug & permission handler
│   ├── descriptors/            # Parser byte-array USB descriptors (Device, Config, IAD, Interface, Endpoint)
│   ├── uac/                    # Resolver topologi UAC 1.0 & UAC 2.0 (Clock Source, Selector, Feature Unit)
│   └── transfer/               # Dual-stage interface claim & jembatan JNI ke native USBFS
├── player/
│   ├── session/                # MediaSessionService & kontrol latar belakang
│   ├── queue/                  # Antrean pemutaran & koordinasi Gapless
│   └── library/                # Pemindai musik lokal & Folder Browser
├── data/
│   ├── local/                  # Sumber data lokal & pembaca metadata
│   └── repository/             # Repositori terpadu untuk UI & Service
├── feature/
│   ├── home/                   # Layar Home
│   ├── library/                # Layar Music Library & Folder Browser
│   ├── player/                 # Layar Now Playing
│   ├── dac/                    # Layar USB DAC Manager & Device Information
│   ├── settings/               # Layar Audio Engine, Audio Settings, & About OpenAudio
│   └── equalizer/              # Layar Equalizer & Parametric EQ
└── native/
    ├── usb/                    # C++17 Direct USB Engine (USBDEVFS ioctl, Isochronous URB ring, Q16.16 Feedback)
    ├── audio/                  # C++17 Lock-free ring buffer (mlock), integer PCM packer, DoP framer
    └── decoder/                # C++17 Native integer decoder bridge
```

---

## Persyaratan Build

- **JDK:** OpenJDK 17 atau lebih baru
- **Android SDK:** API 36 (Android 16), Minimum SDK: API 26 (Android 8.0)
- **Android NDK:** r27c atau r28+ (mendukung 16 KB ELF alignment)
- **CMake:** 3.22.1+

```bash
./gradlew assembleDebug
```

## Lisensi

Proyek ini dilisensikan di bawah **Apache License, Version 2.0**. Lihat [LICENSE](LICENSE) dan [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) untuk informasi lengkap.
