# 🛡️ ScamGuard Verity

**AI-Powered Real-Time Voice Scam & Call Protection for Android**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575.svg?style=flat&logo=materialdesign)](https://m3.material.io/)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📌 Overview

**ScamGuard Verity** is a mobile security application designed to protect users against synthetic voice cloning, deepfake audio impersonation, phone scams, and malicious URLs. 

By combining real-time Android call screening with digital signal processing (DSP) heuristics and machine learning models, ScamGuard Verity analyzes incoming callers and audio recordings in real time, presenting immediate risk scores and call transcripts.

---

## ✨ Key Features

- 📞 **Live Call Screening Overlay:** Intercepts incoming phone calls in real time and displays floating threat alerts directly over the call interface.
- 🎙️ **Synthetic Voice & AI Deepfake Detection:** Analyzes voice characteristics including:
  - **Spectral Centroid:** Identifies unnatural frequency peaks in cloned speech.
  - **Zero Crossing Rate (ZCR):** Detects synthetic audio compression and noise artifacts.
  - **Energy Stability:** Flags robotic or abnormally uniform energy patterns.
  - **Harmonics Ratio:** Evaluates vocal tone clarity versus artificial noise.
- 📝 **Call Transcripts:** Provides auto-generated call transcript summaries for screened phone calls.
- 📄 **PDF Threat Reports:** Generates exportable, shareable PDF reports containing comprehensive technical analysis breakdowns.
- 🔗 **URL & Link Risk Engine:** Analyzes suspicious URLs for phishing indicators, IP host masks, and uncommon domain extensions.
- 🔐 **On-Device AES-256 Encryption:** Protects stored history using `androidx.security.crypto` and hardware-backed Android Keystore.
- 🎨 **Modern Material 3 UI:** Full Jetpack Compose interface with support for Light/Dark themes and smooth state animations.

---

## 🏗️ Architecture & Tech Stack

The application follows **Clean Architecture** principles alongside the **MVVM** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                    │
└────────────────────────────┬────────────────────────────┘
                             │ StateFlow
┌────────────────────────────▼────────────────────────────┐
│                      MainViewModel                      │
└──────┬──────────────────────┬────────────────────┬──────┘
       │                      │                    │
┌──────▼──────┐        ┌──────▼──────┐      ┌──────▼──────┐
│  Use Cases  │        │   Services  │      │ Audio Engine│
└──────┬──────┘        └──────┬──────┘      └──────┬──────┘
       │                      │                    │
┌──────▼──────────────────────▼────────────────────▼──────┐
│      Data Layer (Repositories, DSP, Encryption)         │
└─────────────────────────────────────────────────────────┘
```

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose, Material Design 3
- **Async & Reactive:** Kotlin Coroutines & `StateFlow`
- **Audio Engine:** Jetpack Media3 (ExoPlayer) & Android `MediaRecorder`
- **Telephony & Services:** Android `CallScreeningService`, `WindowManager` Overlay
- **Backend Services:** Firebase Auth & Cloud Functions
- **Local Security:** AndroidX Security Crypto (AES256-GCM)

---

## 📁 Directory Structure

```
app/src/main/java/com/example/scamguardv1/
├── data/                  # Repositories, VoiceAnalyzer, ScamDetector, HistoryManager
│   └── remote/            # API Network Services
├── domain/                # Data models & Business Use Cases
│   ├── model/             # ScreenedCall, DetectionResult, VoiceFeatures, UrlFeatures
│   ├── player/            # VoicePlayer interface
│   └── rules/             # RiskAnalysis & UrlRiskAnalyzer heuristics
├── service/               # ScamCallScreeningService & CallOverlayService
├── ui/                    # Presentation Layer
│   ├── composables/       # AudioComponents, DashboardComposables
│   ├── screens/           # MainScreen, AuthScreen, QrCheckScreen, VoiceRecordingScreen
│   ├── state/             # MainUiState
│   └── theme/             # Color, Type, Theme
└── utils/                 # EncryptionManager, PdfGenerator, PermissionHelper
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** Java 17
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)

### Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/Kentcommit/Verity.git
   cd Verity
   ```

2. **Open in Android Studio:**
   - Select `File > Open` and navigate to the cloned `Verity` directory.
   - Allow Gradle to sync dependencies.

3. **Configure Firebase (Optional):**
   - Place your `google-services.json` inside the `app/` folder for Firebase Authentication features.

4. **Build and Run:**
   - Connect an Android device (or launch an Emulator running API 26+).
   - Press **Run** (`Shift + F10`).

---

## 🔐 Required Permissions

To enable real-time call screening and live overlays, grant the following permissions when prompted:

- **Call Screening Role:** Set ScamGuard Verity as the default Call Screening app.
- **Display over other apps (`SYSTEM_ALERT_WINDOW`):** Enables floating call warning overlays.
- **Microphone (`RECORD_AUDIO`):** Enables local voice recording & live analysis.

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
