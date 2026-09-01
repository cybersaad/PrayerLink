package com.prayerlink.app.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayerlink.app.data.model.CityData
import com.prayerlink.app.data.model.NotificationSoundMode
import com.prayerlink.app.data.model.ThemeMode
import com.prayerlink.app.data.model.UserSettings
import com.prayerlink.app.data.repository.SettingsRepository
import com.prayerlink.app.notification.DailyScheduleWorker
import com.prayerlink.app.notification.PrayerAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LocationStatus { IDLE, DETECTING, DETECTED, FAILED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _locationStatus = MutableStateFlow(LocationStatus.IDLE)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    // ── Update Checker ──
    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _updateResult = MutableStateFlow<com.prayerlink.app.data.model.UpdateResult?>(null)
    val updateResult: StateFlow<com.prayerlink.app.data.model.UpdateResult?> = _updateResult.asStateFlow()

    val currentVersionName: String = try {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    fun checkForUpdates() = viewModelScope.launch {
        _isCheckingForUpdate.value = true
        _updateResult.value = null
        val repo = com.prayerlink.app.data.repository.UpdateRepository()
        val result = repo.checkForUpdate(currentVersionName)
        _updateResult.value = result
        _isCheckingForUpdate.value = false
    }
    
    fun dismissUpdateDialog() {
        _updateResult.value = null
    }

    // ── Update helpers (each persists + reschedules alarms if needed) ──
    
    private suspend fun resetAlarmsAndReminders() {
        androidx.work.WorkManager.getInstance(appContext).cancelAllWorkByTag("interactive_reminder")
        PrayerAlarmScheduler(appContext).scheduleNextPrayer()
    }

    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateNotificationsEnabled(enabled)
        if (enabled) {
            PrayerAlarmScheduler(appContext).scheduleNextPrayer()
            DailyScheduleWorker.enqueue(appContext)
        } else {
            PrayerAlarmScheduler(appContext).cancel()
            androidx.work.WorkManager.getInstance(appContext).cancelAllWorkByTag("interactive_reminder")
        }
    }

    fun updateNotificationSound(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateNotificationSound(enabled)
    }

    fun updateSoundMode(mode: NotificationSoundMode) = viewModelScope.launch {
        settingsRepository.updateSoundMode(mode)
    }

    fun updateCustomAudioUri(uri: String?) = viewModelScope.launch {
        settingsRepository.updateCustomAudioUri(uri)
    }

    fun updateVibration(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateVibration(enabled)
    }

    fun updateTheme(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.updateThemeMode(mode)
    }

    fun updateLanguage(code: String) = viewModelScope.launch {
        settingsRepository.updateLanguage(code)
    }

    fun updateUseGps(useGps: Boolean) = viewModelScope.launch {
        settingsRepository.updateUseGps(useGps)
    }

    fun updateCity(index: Int) = viewModelScope.launch {
        val city = CityData.cities.getOrElse(index) { CityData.cities[0] }
        settingsRepository.updateCity(index, city.latitude, city.longitude, city.timeZoneId, city.displayName(false))
        resetAlarmsAndReminders()
    }

    fun updateCalculationMethod(index: Int) = viewModelScope.launch {
        settingsRepository.updateCalculationMethod(index)
        resetAlarmsAndReminders()
    }

    fun updateAsrJuristic(value: Int) = viewModelScope.launch {
        settingsRepository.updateAsrJuristic(value)
        resetAlarmsAndReminders()
    }

    /** Attempt to detect GPS location. Falls back gracefully. */
    @SuppressLint("MissingPermission")
    fun detectGpsLocation() {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try cached location first
        val cached = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.accuracy }

        if (cached != null) {
            applyLocation(cached)
            return
        }

        // Active request with timeout
        _locationStatus.value = LocationStatus.DETECTING
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                applyLocation(location)
                runCatching { lm.removeUpdates(this) }
            }
            @Deprecated("Deprecated in API") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }

        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
        } catch (_: Exception) {
            _locationStatus.value = LocationStatus.FAILED
            return
        }

        viewModelScope.launch {
            delay(15_000)
            runCatching { lm.removeUpdates(listener) }
            if (_locationStatus.value == LocationStatus.DETECTING) {
                _locationStatus.value = LocationStatus.FAILED
            }
        }
    }

    private fun applyLocation(location: Location) {
        viewModelScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(appContext)
            var locationName = "Current Location"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                            locationName = if (locality != null) "$locality, ${addr.countryCode}" else "Current Location"
                        }
                        finalizeLocationUpdate(location.latitude, location.longitude, locationName)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        locationName = if (locality != null) "$locality, ${addr.countryCode}" else "Current Location"
                    }
                    finalizeLocationUpdate(location.latitude, location.longitude, locationName)
                }
            } catch (e: Exception) {
                finalizeLocationUpdate(location.latitude, location.longitude, locationName)
            }
        }
    }

    private fun finalizeLocationUpdate(lat: Double, lon: Double, locationName: String) {
        _locationStatus.value = LocationStatus.DETECTED
        viewModelScope.launch {
            val tz = java.time.ZoneId.systemDefault().id
            settingsRepository.updateLocation(lat, lon, tz, locationName)
            resetAlarmsAndReminders()
        }
    }
}
