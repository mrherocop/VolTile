# VolTile 📱🎵

[![Android CI/CD](https://github.com/mrherocop/VolTile/actions/workflows/android.yml/badge.svg)](https://github.com/mrherocop/VolTile/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B-green.svg)](#)

**VolTile** is a minimalist, modern, and high-performance Android volume management utility built 100% in Kotlin using Jetpack Compose and Material 3 design guidelines. 

It provides direct, tactile volume adjustments for all system audio streams via a system Quick Settings (QS) Tile. By design, VolTile preserves your screen real estate and enforces a **zero floating icons** policy—running invisibly in the background until invoked.

---

## 🚀 Key Features

- **Quick Settings Tile Integration**: Toggle your volume controls instantly with one swipe and tap from any screen.
- **Granular Audio Mixing**: Control all 5 standard system audio streams:
  - 🎵 **Media** (Music, videos, games)
  - 🔔 **Ring** (Incoming calls)
  - ⏰ **Alarm** (Wake-up alerts)
  - 💬 **Notification** (System messages)
  - 📞 **Voice Call** (Active call earpiece)
- **Zero Floating Widgets**: No annoying, persistent overlay bubbles. It utilizes a clean Material 3 `ModalBottomSheet` overlay that dismisses immediately after adjustment.
- **Per-App Volume Mixers (Future)**: Built-in structural foundation (MVVM repository pattern) for application-specific volume settings.
- **Battery-Optimized Background Service**: Utilizes a low-impact Android Foreground Service with min-importance notification to guarantee immediate tile response without battery drain.
- **Reboot Persistence**: Automatically restarts the background service when the phone reboots.

---

## 📸 User Interface & Experience

VolTile features a curated dark-mode theme utilizing high-contrast Material 3 sliders, dynamic icon feedback, and smooth transitions:

- **Tactile Sliders**: Slider track indicators dynamically show exact volume percentages.
- **Icon Actions**: Tap any stream icon to instantly mute/unmute that specific channel.
- **Auto-Dismiss**: Outside-click or down-drag to dismiss, returning you instantly to your previous application.

---

## 📦 Installation & Download

To install VolTile on your Android device (Android 10+ / API 29+):

1. **Download the APK**: Go to the [Releases](https://github.com/mrherocop/VolTile/releases) section and download the latest `app-debug.apk`.
2. **Install**: Open the downloaded APK on your device. (You may need to allow installation from unknown sources in your browser or file manager settings).
3. **Add the Tile**:
   - Swipe down twice from the top of your screen to open the Quick Settings panel.
   - Tap the **Edit (Pencil)** icon.
   - Scroll down to find the **VolTile** tile and drag it into your active layout.
   - Tap the **VolTile** tile anytime to control your volume.

---

## 🛠️ Architecture & Tech Stack

VolTile is architected following modern Android development best practices:

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material 3 components
- **Architecture**: MVVM (Model-View-ViewModel) + Repository pattern
- **State Management**: Kotlin `StateFlow` / `SharedFlow` for reactive, thread-safe UI updates
- **Core APIs**: `AudioManager` for direct hardware audio channel manipulation
- **Background Operations**: `ForegroundService` with `mediaPlayback` type compatibility to comply with Android 14+ background execution policies.

---

## 💻 Local Development

### Prerequisites
- Android Studio (Koala or newer recommended)
- Android SDK 35 (Android 15)
- JDK 17

### Build Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/mrherocop/VolTile.git
   cd VolTile
   ```
2. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run on your connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
