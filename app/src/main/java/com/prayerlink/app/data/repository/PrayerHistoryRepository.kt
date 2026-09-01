package com.prayerlink.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prayerlink.app.data.local.PrayerCompletionDao
import com.prayerlink.app.data.local.PrayerCompletionEntity
import com.prayerlink.app.data.model.CompletionSource
import com.prayerlink.app.data.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "prayer_history")

@Singleton
class PrayerHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PrayerCompletionDao
) {
    private val COMPLETED_PRAYERS_KEY = stringSetPreferencesKey("completed_prayers")
    private val MIGRATION_COMPLETE_KEY = booleanPreferencesKey("room_migration_complete")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            migrateLegacyDataStoreToRoom()
        }
    }

    private suspend fun migrateLegacyDataStoreToRoom() {
        val prefs = context.historyDataStore.data.first()
        val isMigrated = prefs[MIGRATION_COMPLETE_KEY] ?: false

        if (!isMigrated) {
            val legacySet = prefs[COMPLETED_PRAYERS_KEY] ?: emptySet()
            val entities = legacySet.mapNotNull { key ->
                val parts = key.split("_")
                if (parts.size == 2) {
                    val dateStr = parts[0]
                    val prayerStr = parts[1]
                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                    val prayer = runCatching { Prayer.valueOf(prayerStr) }.getOrNull()
                    if (date != null && prayer != null) {
                        PrayerCompletionEntity(
                            id = key,
                            date = date,
                            prayer = prayer,
                            completed = true,
                            completedAt = null, // Legacy has no timestamp
                            reminderAnswered = false,
                            source = CompletionSource.IMPORT,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else null
                } else null
            }
            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
            }
            context.historyDataStore.edit {
                it[MIGRATION_COMPLETE_KEY] = true
            }
        }
    }

    suspend fun markPrayerCompleted(date: LocalDate, prayer: Prayer, source: CompletionSource) {
        val id = "${date}_${prayer.name}"
        val entity = PrayerCompletionEntity(
            id = id,
            date = date,
            prayer = prayer,
            completed = true,
            completedAt = System.currentTimeMillis(),
            reminderAnswered = false,
            source = source,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    suspend fun isPrayerCompleted(date: LocalDate, prayer: Prayer): Boolean {
        val id = "${date}_${prayer.name}"
        val record = dao.getPrayer(id)
        return record?.completed == true
    }

    fun getAllHistory(): Flow<List<PrayerCompletionEntity>> {
        return dao.getAllHistory()
    }

    fun getHistoryForMonth(year: Int, month: Int): Flow<List<PrayerCompletionEntity>> {
        val start = LocalDate.of(year, month, 1).toEpochDay()
        val end = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth()).toEpochDay()
        return dao.getHistoryForDateRange(start, end)
    }

    fun getAllCompletedDays(): Flow<List<Long>> {
        return dao.getAllCompletedDays()
    }
}
