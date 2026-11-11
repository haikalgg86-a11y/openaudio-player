# Third-Party Notices & License Attributions

**OpenAudio Player** is licensed under the **Apache License, Version 2.0**.
All third-party libraries and headers used or referenced by this project have been audited to ensure strict compatibility with the Apache License 2.0, without any GPL, AGPL, or LGPL copyleft obligations in the distributed application binary.

---

## 1. Android Jetpack, Kotlin, & Google Libraries
- **Components:** AndroidX Core, Activity Compose, Lifecycle, Navigation Compose, Material3, Room, DataStore Preferences, Media3 Session, Kotlinx Coroutines, Kotlin Standard Library.
- **License:** Apache License, Version 2.0
- **Source:** [https://developer.android.com/jetpack](https://developer.android.com/jetpack)

---

## 2. Xiph.Org Audio Codec Libraries (`libFLAC`, `libogg`, `libopus`, `libvorbis`)
- **Components:** `libFLAC` (library-only source from `src/libFLAC`, excluding GPL CLI tools), `libogg`, `libopus`, `libvorbis`.
- **License:** Xiph.Org BSD-Style License (`BSD-3-Clause` / `COPYING.Xiph`)
- **Source:** [https://github.com/xiph/flac](https://github.com/xiph/flac)

```text
Copyright (C) 2000-2009  Josh Coalson
Copyright (C) 2011-2025  Xiph.Org Foundation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of the Xiph.org Foundation nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.
```

---

## 3. Single-File Public Domain / MIT-0 Audio Decoders (`dr_libs`)
- **Components:** `dr_wav`, `dr_mp3`
- **Author:** David Reid (Mackron)
- **License:** MIT-0 OR Public Domain (Unlicense)

---

## 4. Linux Kernel UAPI Headers (`<linux/usbdevice_fs.h>`, `<linux/usb/ch9.h>`, `<linux/usb/audio.h>`)
- **Components:** Standard Linux Userspace ABI headers shipped with the Android NDK Sysroot.
- **License:** `GPL-2.0 WITH Linux-syscall-note`
- **Notice:** Pursuant to the `Linux-syscall-note` exception, normal user-space programs that use kernel services via normal system calls and `ioctl` definitions from these headers are NOT considered derivative works of the kernel and are distributed under the project's Apache License 2.0.
