<p align="center">
  <img 
    width="220" 
    height="220" 
    alt="PrayerLink Logo" 
    src="https://raw.githubusercontent.com/cybersaad/PrayerLink/main/prayerlink_logo.svg" 
  />
</p>

<h1 align="center">PrayerLink</h1>

<p align="center">
  A beautiful, privacy-first, and offline-capable Android application for precise daily prayers and smart reminders
</p>

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Release](https://img.shields.io/badge/Release-v1.0-161B22?style=for-the-badge&logo=github&logoColor=white)


PrayerLink v1.0 is an elegant and entirely offline Android application designed to help Muslims around the world maintain their daily prayers with precision. Built entirely with **Kotlin** and **Jetpack Compose**, this app provides offline mathematical prayer calculations, Doze-resistant Adhan services, and an interactive prayer activity dashboard to track your consistency.

<img width="379" height="788" alt="PrayerLink Dashboard" src="https://via.placeholder.com/379x788.png?text=PrayerLink+Dashboard" />

</div>

---

## Features

### Core Features
- **Offline Prayer Calculations:** Mathematical precision without needing an internet connection. Adjusts to your GPS or manually selected city.
- **Background Adhan Service:** Reliable, Doze-resistant foreground service that ensures you never miss a prayer.
- **Interactive Reminders:** "Did you pray?" notifications utilizing `AlarmManager` and `WorkManager` for delayed follow-ups and missed prayer tracking.
- **Prayer Activity Calendar:** A gorgeous, GitHub-style contribution graph right on your dashboard to track streaks and perfect days.
- **Qaza (Overdue) Tracking:** Visual warnings and tracking for prayers approaching their end times.

### Localization & Customization
- **Multi-lingual & RTL:** Seamlessly switch between English and Arabic (`العربية`).
- **Dynamic Themes:** Full support for Light, Dark, and System Default themes using Material 3 guidelines.
- **Custom Audio:** Choose between default Makkah Adhan, system notification sounds, or pick your own custom audio file.

### Advanced Update System
- **In-App GitHub Updates:** An entirely offline-friendly, independent update checker that securely queries the GitHub API to notify you of new releases.
- **Smart Notifications:** The app intelligently tracks update notifications so you aren't spammed for the same version twice.
- **Secure Downloads:** Clicking "Update Now" safely opens the official GitHub Release page in your browser, keeping your device secure from background installations.

### Privacy First
- **Zero Telemetry:** 100% of your data (settings, location, prayer history) stays strictly on your device using encrypted DataStore and Room Database.
- **Minimal Permissions:** We only ask for what we need. Location is optional. No ads. No tracking.

### Permissions

| Permission | Required For | Note |
| :--- | :--- | :--- |
| `INTERNET` | Update notification check | The app checks GitHub Releases to notify you of new versions. **All prayer calculations and reminders work fully offline.** |
| `ACCESS_COARSE_LOCATION` | Automatic City Detection | Only requested if you enable "Automatic Location (GPS)" to calculate solar angles accurately. |
| `POST_NOTIFICATIONS` | Adhan & Reminders | Required on Android 13+ to display the Adhan, Reminders, and App Updates. |
| `SCHEDULE_EXACT_ALARM` | Precision Timing | Required on Android 14+ to guarantee precision prayer timing. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Adhan Audio | To reliably play the Adhan while the app is closed. |

---

## Installation

### Recommended (Direct Install)
The fastest way to get the app on your phone:

1. **Download the APK:** [Click here to download the latest `.apk`](https://github.com/cybersaad/PrayerLink/releases/latest) (or go to the **Releases** section).
2. **Open the file:** Tap the downloaded `.apk` file on your Android device.
3. **Allow Installation:** If prompted, allow "Install from unknown sources" in your security settings.
4. **Done!** You're ready to start tracking your prayers.

---

## Building from Source

If you want to contribute or build the app yourself:

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer.
- **JDK 17** or higher.
- **Android SDK 34** installed.

### Steps
1. **Clone the repo:**
   ```bash
   git clone https://github.com/cybersaad/PrayerLink.git
   ```
2. **Open in Android Studio:** Choose `Open` and select the project folder.
3. **Sync Gradle:** Wait for the project to download all necessary dependencies.
4. **Run:** Connect your device/emulator and click the **Run** button.

---

## Project Architecture

The app follows a clean, modular MVVM structure for maintainability:

```text
com.prayerlink.app/
├── config/                   # Centralized application config (GitHub Repositories)
├── data/                     # Data Layer (Repositories, Room, DataStore, Calculation Engine)
├── di/                       # Dependency Injection (Hilt Modules)
├── notification/             # Background Services, Receivers, WorkManager for Adhan & Updates
├── ui/                       # Presentation Layer
│   ├── dashboard/            # Dashboard UI, Activity Calendar, Prayer tracking
│   ├── settings/             # Settings UI, Location selection, Update dialog
│   ├── components/           # Reusable Jetpack Compose components
│   └── theme/                # Material3 typography, colors, and shapes
└── PrayerLinkApplication.kt  # App entry point
```

- **`data/`**: Pure offline logic and storage handling. All prayer math resides here.
- **`notification/`**: The robust engine ensuring alarms fire exactly on time, independent of UI.
- **`ui/`**: 100% Jetpack Compose reactive interface. 

---

## Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material3) |
| **Architecture** | MVVM (State-driven UI) |
| **Dependency Injection** | Dagger Hilt |
| **Local Storage** | Jetpack DataStore & Room SQL |
| **Background Tasks** | WorkManager, AlarmManager, Foreground Services |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 34 (Android 14) |

---

## Version History

| Version | Highlights |
| :--- | :--- |
| **1.0** | Initial Release — Offline calculation, Adhan services, Activity Calendar, and GitHub Update Checker. |

---

## Contribution

Contributions are welcome! Please ensure that you run the linting (`./gradlew lintDebug`) and unit tests (`./gradlew testDebugUnitTest`) before submitting a Pull Request.

---

## License

**© 2026 Saad Khan.** All rights reserved.
Developed with ❤️ by [Saad Khan](https://github.com/cybersaad).
