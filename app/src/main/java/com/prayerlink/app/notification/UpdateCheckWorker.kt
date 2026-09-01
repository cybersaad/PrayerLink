package com.prayerlink.app.notification

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.prayerlink.app.data.datasource.PreferencesKeys
import com.prayerlink.app.data.datasource.prayerLinkDataStore
import com.prayerlink.app.data.model.UpdateResult
import com.prayerlink.app.data.repository.UpdateRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import androidx.datastore.preferences.core.edit

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val updateRepo = UpdateRepository()
            
            // Get current version name safely
            val currentVersionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0.0"
            }

            val result = updateRepo.checkForUpdate(currentVersionName)

            if (result is UpdateResult.NewUpdateAvailable) {
                val prefs = context.prayerLinkDataStore.data.first()
                val lastNotifiedVersion = prefs[PreferencesKeys.LAST_NOTIFIED_VERSION]
                val latestVersion = result.release.version

                // Update DataStore with the latest known version
                context.prayerLinkDataStore.edit { preferences ->
                    preferences[PreferencesKeys.LATEST_KNOWN_VERSION] = latestVersion
                    preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIME] = System.currentTimeMillis()
                }

                // If we haven't notified for this specific version yet, notify now
                if (lastNotifiedVersion != latestVersion) {
                    PrayerNotificationManager(context).showUpdateAvailableNotification(latestVersion)
                    
                    context.prayerLinkDataStore.edit { preferences ->
                        preferences[PreferencesKeys.LAST_NOTIFIED_VERSION] = latestVersion
                    }
                }
            } else if (result is UpdateResult.UpToDate) {
                 context.prayerLinkDataStore.edit { preferences ->
                    preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIME] = System.currentTimeMillis()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "github_update_check"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
