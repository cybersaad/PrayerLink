# PrayerLink 🕌

PrayerLink is a beautiful, privacy-first, and offline-capable Android application designed to help Muslims around the world maintain their daily prayers with precision and elegance. Built entirely with Kotlin, Jetpack Compose, and Material Design 3.

## Features ✨

### Core Features
- **Offline Prayer Calculations:** Mathematical precision without needing an internet connection. Adjusts to your GPS or manually selected city.
- **Background Adhan Service:** Reliable, Doze-resistant foreground service that ensures you never miss a prayer.
- **Interactive Reminders:** "Did you pray?" notifications utilizing AlarmManager and WorkManager for delayed follow-ups and missed prayer tracking.
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
- **Minimal Permissions:** We only ask for what we need. Location is optional.

## Installation 🚀

You can build the project from source or download the latest APK from the [Releases](https://github.com/cybersaad/PrayerLink/releases) tab.

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17+
- Android SDK 34

### Build Instructions
```bash
git clone https://github.com/cybersaad/PrayerLink.git
cd PrayerLink

# To build a debug APK:
./gradlew assembleDebug
# The APK will be available in: app/build/outputs/apk/debug/app-debug.apk

# To build a release APK (requires keystore configuration):
./gradlew assembleRelease
# The APK will be available in: app/build/outputs/apk/release/app-release.apk
```

## Architecture & Technologies 🏗

- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Dagger Hilt
- **Local Storage:** Jetpack DataStore (Preferences) & Room (Local SQL Database)
- **Background Work:** WorkManager & AlarmManager & Foreground Services
- **Concurrency:** Kotlin Coroutines & Flow

## Folder Structure 📁

- `config/`: Centralized application configuration (e.g. GitHub Repository identifiers).
- `data/`: Repositories, Models, DataStore, Room entities, and mathematical calculation engines.
- `di/`: Hilt Modules for dependency injection.
- `notification/`: Services, Receivers, and Workers handling the Adhan, prayer reminders, and the update checker.
- `ui/`: Compose UI split by feature (dashboard, settings, navigation, components).

## Permissions 🔒

We respect your privacy. The app requests minimal permissions:
- `INTERNET`: Exclusively used to check for new app versions via the GitHub API.
- `ACCESS_COARSE_LOCATION`: Only requested if you enable "Automatic Location (GPS)" to calculate solar angles accurately.
- `POST_NOTIFICATIONS`: Required on Android 13+ to display the Adhan, Reminders, and App Updates.
- `SCHEDULE_EXACT_ALARM`: Required on Android 14+ to guarantee precision prayer timing.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: To play the Adhan while the app is closed.

## Testing 🧪

To run the unit tests (which cover version comparison logic, date boundaries, and more):
```bash
./gradlew testDebugUnitTest
```

## Contributing 🤝

Contributions are welcome! Please ensure that you run the linting and unit tests before submitting a Pull Request.

*Note: This app relies on the AlAdhan API offline mathematical parameters for calculating precise timings. No internet is used for prayer calculations.*
