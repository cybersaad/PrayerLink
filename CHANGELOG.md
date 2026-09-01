# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-07-23

### Added
- **Accurate Prayer Calculations**: Offline prayer times calculated accurately using standard geographical formulas for multiple conventions (MWL, ISNA, Egypt, Makkah, Karachi).
- **Foreground Adhan Service**: High-priority alarm notification that safely plays the Makkah Adhan without being killed by Android's Doze mode.
- **Interactive Reminders**: "Did you pray?" notifications using WorkManager that allow users to mark prayers as completed directly from the lock screen.
- **Prayer Activity Calendar**: A GitHub-style contribution grid visualization on the Dashboard that tracks 5/5 perfect prayer days and daily streaks, backed by persistent local DataStore.
- **Material 3 Design**: Fully responsive Material Design 3 UI using Jetpack Compose, supporting smooth transitions, dynamic colors, and Light/Dark modes.
- **Location Detection**: Built-in GPS polling or offline manual city selection with timezone synchronization.
- **Bilingual Support**: Instant switching between English and Arabic (`ar`) locales without app restarts.
- **Performance Optimizations**: IO-offloaded Coroutine flows ensure zero UI-thread blocking during rapid clock ticks.

### Security & Compliance
- Removed all unnecessary permissions.
- Implemented `FLAG_IMMUTABLE` on all `PendingIntents`.
- Added exact alarm (`SCHEDULE_EXACT_ALARM`) and foreground service permissions specifically restricted to Android 14+ best practices.

*This is the initial commercial release for Google Play.*
