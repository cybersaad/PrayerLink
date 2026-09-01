package com.prayerlink.app.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for the entire application.
 *
 * **Important:** `preferencesDataStore` must be declared exactly once
 * as a top-level extension. All access must go through this property
 * to avoid "multiple DataStores active for the same file" crashes.
 */
val Context.prayerLinkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "prayer_link_settings"
)

/**
 * Centralized DataStore preference keys.
 */
object PreferencesKeys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val NOTIFICATION_SOUND    = booleanPreferencesKey("notification_sound")
    val SOUND_MODE            = stringPreferencesKey("sound_mode")
    val CUSTOM_AUDIO_URI      = stringPreferencesKey("custom_audio_uri")
    val VIBRATION_ENABLED     = booleanPreferencesKey("vibration_enabled")
    val THEME_MODE            = stringPreferencesKey("theme_mode")
    val LANGUAGE              = stringPreferencesKey("language")
    val USE_GPS               = booleanPreferencesKey("use_gps")
    val SELECTED_CITY_INDEX   = intPreferencesKey("selected_city_index")
    val CALC_METHOD_INDEX     = intPreferencesKey("calculation_method_index")
    val LATITUDE              = doublePreferencesKey("latitude")
    val LONGITUDE             = doublePreferencesKey("longitude")
    val TIME_ZONE_ID          = stringPreferencesKey("time_zone_id")
    val LOCATION_NAME         = stringPreferencesKey("location_name")
    val ASR_JURISTIC          = intPreferencesKey("asr_juristic")

    // Update Checker
    val LATEST_KNOWN_VERSION    = stringPreferencesKey("latest_known_version")
    val LAST_UPDATE_CHECK_TIME  = androidx.datastore.preferences.core.longPreferencesKey("last_update_check_time")
    val LAST_NOTIFIED_VERSION   = stringPreferencesKey("last_notified_version")
}
