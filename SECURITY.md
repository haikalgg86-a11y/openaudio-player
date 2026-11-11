# Kebijakan Keamanan (Security Policy)

## Versi yang Didukung

| Versi | Status Dukungan Keamanan | Target Android SDK |
| :--- | :--- | :--- |
| `0.1.x` (Development) | Didukung Aktif | API 26 – API 36 (Android 8.0 – Android 16) |

## Prinsip Keamanan USB Host & Native C++ (`usbdevfs`)

OpenAudio Player berkomunikasi langsung dengan perangkat keras USB DAC melalui Android `UsbManager` dan ioctl kernel Linux `usbdevfs` di lapisan C++17:

1. **Tanpa Eksploitasi Privilege / Tanpa Root:**
   - Aplikasi berjalan sepenuhnya di dalam sandbox Android standar (`untrusted_app`) dan hanya mengakses file descriptor yang diberikan secara sah oleh pengguna melalui dialog izin `UsbManager.requestPermission()`.
2. **Validasi Batas Memori Descriptor USB (Anti-Malformed Descriptor):**
   - Perangkat USB yang rusak atau berbahaya dapat mengirimkan USB descriptor dengan nilai `bLength` atau `wTotalLength` yang tidak valid. Seluruh parser descriptor di `usb:descriptors` dan `native:usb` memvalidasi batas indeks array sebelum membaca byte untuk mencegah *buffer over-read* atau *heap corruption*.
3. **Pencegahan Kebocoran File Descriptor & Penguncian Audio Sistem:**
   - Setiap `UsbDeviceConnection` dan klaim `PreferredMixerAttributes` wajib dilepaskan (`clearPreferredMixerAttributes` dan `close()`) saat pemutaran berhenti, perangkat dicabut, atau layanan dihentikan.

## Melaporkan Kerentanan

Jika Anda menemukan celah keamanan (seperti potensi *buffer overflow* pada parser descriptor USB atau parser header audio), mohon jangan membuka *issue* publik terlebih dahulu. Kirimkan laporan detail melalui fitur *Private Vulnerability Reporting* di tab **Security** repositori GitHub OpenAudio Player.
