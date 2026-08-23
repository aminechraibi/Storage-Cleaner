# 🧹 Android Storage Cleaner & Cache Optimizer (Open Source)

> **The fast, lightweight, and 100% privacy-friendly storage cleaner & junk file remover for Android.** Optimized for all phones, including low-RAM and low-end devices. Built with modern **Kotlin** and **Jetpack Compose**.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Clean Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange.svg)](#architecture--tech-stack)
[![No Telemetry](https://img.shields.io/badge/Privacy-100%25%20On--Device-success.svg)](#-privacy--safety-first)

---

## ☕ Support

Found this useful? A coffee goes a long way ☕

<a href='https://ko-fi.com/P5P21ZQGK2' target='_blank'><img height='72' style='border:0px;height:72px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

---

## ⚡ Why Choose Storage Cleaner?

Unlike commercial cleaner apps that track user data, run battery-draining background daemons, or push intrusive ads, **Storage Cleaner** is designed from the ground up to be:
- 🔒 **100% Private & Offline**: Zero analytics, zero ad SDKs, zero telemetry, and zero internet requests for scanning.
- ⚡ **No Background Battery Drain**: No background services or scheduled silent wakeups. Cleanup runs **only when you ask it to**.
- 📱 **Low-End Phone Friendly**: Streaming memory pipelines prevent `OutOfMemory` (OOM) crashes even on 2GB/3GB RAM devices.
- 🗑️ **Safe Recycle Bin**: Move unwanted files to a non-destructive quarantine bin before permanent deletion.
- 🎨 **Modern Material 3 UI**: Clean minimalism with high-contrast Light theme, Dark mode, and tactile haptic feedback.

---

## 🚀 Key Features

### 1. ⚡ App Cache & Storage Manager
* **Batch Cache Cleaner**: Multi-select installed applications or select all with one tap to clear accumulated temporary cache files.
* **Granular Breakdown**: View detailed app APK size, data size, and cache memory usage per application.
* **Direct Settings Shortcut**: Instant one-tap access to Android system storage settings for deep system app management.

### 2. 🔍 Junk & Temp Files Remover
* **Quick & Deep Cleaning**: Scan for obsolete `.apk` installers, temporary log files, thumbnail caches (`.thumbnails`), empty folders, and orphaned directories.
* **Residual File Detection**: Automatically detect leftover directories from uninstalled apps in `/Android/data` and `/Android/media`.

### 3. 🖼️ Duplicate & Similar Photos Finder
* **Fast Duplicate Detection**: Two-phase chunk hashing + full cryptographic SHA-256 validation to safely group 100% identical files.
* **Similar Photos Engine**: Perceptual average color bucket analysis to identify burst shots, repeated camera photos, and similar memes.

### 4. 📦 Large Files & Old Files Analyzer
* **Custom Storage Thresholds**: Filter files exceeding 10MB, 25MB, 50MB, or 100MB+.
* **Old File Stale Scanner**: Find downloads and documents untouched for 30, 60, 90, or 180+ days.

### 5. 🛡️ Whitelist & Exclusion Shield
* **Protected Paths**: Add sensitive directories (e.g., WhatsApp Media, Camera DCIM, Documents) to your whitelist so they are never touched during automated cleanups.

### 6. 📜 Cleanup History & Reclaimed Space Tracker
* **Audit Logs**: Room database records every cleanup session, items deleted, and total megabytes/gigabytes saved over time.

---

## 🛡️ Privacy & Safety First

| Principle | How We Enforce It |
| :--- | :--- |
| **No Background Tasks** | No `WorkManager` background alarms or persistent broadcast listeners. |
| **No Data Collection** | 100% on-device processing via local MediaStore & StorageStats APIs. |
| **Explicit Confirmation** | Dangerous actions require confirmation dialogs and manual selection. |
| **Recycle Bin Safe-guard** | Deleted files can be routed to an internal recycle bin for easy recovery. |

---

## 🛠️ Architecture & Tech Stack

```text
├── domain/            # Clean Architecture Use Cases & Repository Interfaces
├── data/              # Room DB, MediaStore Scanners, StorageStats, DataStore
└── ui/                # Jetpack Compose Screens, MVVM ViewModels, M3 Theme
```

* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
* **Architecture**: MVVM + Clean Architecture (SOLID, Repository Pattern)
* **Local Database**: [Room](https://developer.android.com/training/data-storage/room) with SQLite & FTS support
* **Storage Engine**: `StorageStatsManager`, `MediaStore`, Storage Access Framework (SAF)
* **Preferences**: `DataStore Preferences`
* **Concurrency**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)

---

## 📥 Installation & Releases

Download the latest pre-compiled signed APK directly from the [**GitHub Releases**](https://github.com/) tab.

### Local Compilation

```bash
# Clone the repository
git clone https://github.com/your-username/android-storage-cleaner.git

# Navigate to project directory
cd android-storage-cleaner

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## 🤝 Contributing

Contributions, feature suggestions, and pull requests are warmly welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **Apache 2.0 License**. See `LICENSE` for more information.
