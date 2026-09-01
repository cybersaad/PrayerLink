package com.prayerlink.app.data.model

/**
 * User-configurable theme modes following Material 3 guidelines.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class NotificationSoundMode {
    ADHAN,
    SYSTEM,
    CUSTOM
}

/**
 * All user-configurable preferences persisted via DataStore.
 *
 * Default values represent sane first-launch settings
 * (Makkah coordinates, MWL calculation method, Shafi Asr).
 */
data class UserSettings(
    val notificationsEnabled: Boolean = true,
    @Deprecated("Use soundMode instead")
    val notificationSound: Boolean = true, 
    val soundMode: NotificationSoundMode = NotificationSoundMode.ADHAN,
    val customAudioUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "en",
    val useGps: Boolean = false,
    val selectedCityIndex: Int = 0,
    val calculationMethodIndex: Int = 0,
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val timeZoneId: String = "Asia/Riyadh",
    val locationName: String = "Makkah, SA",
    val asrJuristic: Int = 0,   // 0 = Standard (Shafi), 1 = Hanafi
    val latestKnownVersion: String? = null
)
