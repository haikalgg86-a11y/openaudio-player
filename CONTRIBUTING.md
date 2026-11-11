# Panduan Kontribusi (Contributing to OpenAudio Player)

Terima kasih atas minat Anda untuk berkontribusi pada **OpenAudio Player**! Proyek ini bertujuan menyediakan pemutar musik Hi-Res dan mesin USB Audio Class (UAC 1.0 & UAC 2.0) yang transparan, terverifikasi secara ilmiah, dan sepenuhnya bebas/open-source di bawah **Apache License 2.0**.

## Prinsip Utama Rekayasa Audio & Kode

1. **Kejujuran Teknis Tanpa Klaim Palsu:**
   - Jangan pernah menampilkan status `Bit-Perfect` hanya berdasarkan kecocokan angka *sample rate* pada UI.
   - Selalu gunakan sistem 5 tingkat verifikasi (`PCM_PIPELINE_VERIFIED`, `USB_URB_SUBMISSION_VERIFIED`, `USB_TRANSFER_COMPLETION_VERIFIED`, `ANDROID_HAL_BIT_PERFECT_REPORTED`, `HARDWARE_OUTPUT_UNVERIFIED`).
2. **Larangan Ketat Menyalin Kode Komersial / Lisensi Tidak Kompatibel:**
   - Seluruh kode harus merupakan implementasi *clean-room* berdasarkan dokumentasi resmi [Android Developers](https://developer.android.com), [USB-IF Audio Class 1.0/2.0 Specifications](https://www.usb.org/documents), dan [Linux Kernel USBFS ABI](https://docs.kernel.org/driver-api/usb/usb.html).
   - Jangan menambahkan dependency berlisensi GPL, AGPL, atau LGPL tanpa diskusi dan persetujuan arsitektur terlebih dahulu.
3. **Kepatuhan Android 16 (API 36) & 16 KB Page Size:**
   - Seluruh modul native C++17 wajib mempertahankan flag linker `-Wl,-z,max-page-size=16384` dan menggunakan `sysconf(_SC_PAGESIZE)` saat runtime.
   - Jangan melakukan alokasi memori heap (`new` / `malloc`) atau pemblokiran mutex di dalam loop *real-time* pengiriman URB isochronous.
4. **Pengujian Perangkat Keras Nyata:**
   - Perubahan pada modul `usb/*` dan `native/usb/*` wajib disertai log pengujian dari perangkat Android fisik dan USB DAC nyata (sertakan `VID:PID`, versi UAC, versi ROM Android, dan output diagnostik). Emulator tidak boleh dijadikan bukti keberhasilan Direct USB Audio Engine.

## Alur Pengajuan Pull Request

1. Fork repositori ini dan buat branch fitur/perbaikan dari `main`.
2. Pastikan kode mematuhi pemisahan modul (jangan menaruh logika USB atau decoding di dalam modul `feature/*` atau `MainActivity`).
3. Jalankan pemeriksaan build dan uji unit sebelum membuka Pull Request.
4. Dengan mengirimkan kontribusi, Anda menyetujui bahwa kontribusi Anda dilisensikan di bawah **Apache License 2.0** sesuai Bagian 5 dari teks lisensi.
