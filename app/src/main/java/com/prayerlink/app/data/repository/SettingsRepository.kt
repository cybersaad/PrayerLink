package com.prayerlink.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.model.NotificationSoundMode
import com.prayerlink.app.data.model.ThemeMode
import com.prayerlink.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps DataStore for reactive, type-safe access
 * to all user preferences.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** Emits the latest [UserSettings] whenever any preference changes. */
    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            notificationsEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            notificationSound = prefs[PreferencesKeys.NOTIFICATION_SOUND] ?: true,
            soundMode = runCatching {
                NotificationSoundMode.valueOf(prefs[PreferencesKeys.SOUND_MODE] ?: NotificationSoundMode.ADHAN.name)
            }.getOrDefault(NotificationSoundMode.ADHAN),
            customAudioUri = prefs[PreferencesKeys.CUSTOM_AUDIO_URI],
            vibrationEnabled = prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            language = prefs[PreferencesKeys.LANGUAGE] ?: "en",
            useGps = prefs[PreferencesKeys.USE_GPS] ?: false,
            selectedCityIndex = prefs[PreferencesKeys.SELECTED_CITY_INDEX] ?: 0,
            calculationMethodIndex = prefs[PreferencesKeys.CALC_METHOD_INDEX] ?: 0,
            latitude = prefs[PreferencesKeys.LATITUDE] ?: 21.4225,
            longitude = prefs[PreferencesKeys.LONGITUDE] ?: 39.8262,
            timeZoneId = prefs[PreferencesKeys.TIME_ZONE_ID] ?: "Asia/Riyadh",
            locationName = prefs[PreferencesKeys.LOCATION_NAME] ?: "Makkah, SA",
            asrJuristic = prefs[PreferencesKeys.ASR_JURISTIC] ?: 0,
            latestKnownVersion = prefs[PreferencesKeys.LATEST_KNOWN_VERSION]
        )
    }

    // ── Individual update helpers ───────────────────────────────

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotificationSound(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION_SOUND] = enabled }
    }

    suspend fun updateSoundMode(mode: NotificationSoundMode) {
        dataStore.edit { it[PreferencesKeys.SOUND_MODE] = mode.name }
    }

    suspend fun updateCustomAudioUri(uri: String?) {
        dataStore.edit {
            if (uri != null) {
                it[PreferencesKeys.CUSTOM_AUDIO_URI] = uri
            } else {
                it.remove(PreferencesKeys.CUSTOM_AUDIO_URI)
            }
        }
    }

    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updateLanguage(code: String) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = code }
    }

    suspend fun updateUseGps(useGps: Boolean) {
        dataStore.edit { it[PreferencesKeys.USE_GPS] = useGps }
    }

    suspend fun updateCity(index: Int, lat: Double, lon: Double, timeZoneId: String, locationName: String) {
        dataStore.edit {
            it[PreferencesKeys.SELECTED_CITY_INDEX] = index
            it[PreferencesKeys.LATITUDE] = lat
            it[PreferencesKeys.LONGITUDE] = lon
            it[PreferencesKeys.TIME_ZONE_ID] = timeZoneId
            it[PreferencesKeys.LOCATION_NAME] = locationName
        }
    }

    suspend fun updateLocation(lat: Double, lon: Double, timeZoneId: String, locationName: String) {
        dataStore.edit {
            it[PreferencesKeys.LATITUDE] = lat
            it[PreferencesKeys.LONGITUDE] = lon
            it[PreferencesKeys.TIME_ZONE_ID] = timeZoneId
            it[PreferencesKeys.LOCATION_NAME] = locationName
        }
    }

    suspend fun updateCalculationMethod(index: Int) {
        dataStore.edit { it[PreferencesKeys.CALC_METHOD_INDEX] = index }
    }

    suspend fun updateAsrJuristic(value: Int) {
        dataStore.edit { it[PreferencesKeys.ASR_JURISTIC] = value }
    }
}
