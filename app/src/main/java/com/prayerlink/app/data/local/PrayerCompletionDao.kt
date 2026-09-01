package com.prayerlink.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PrayerCompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: PrayerCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(completions: List<PrayerCompletionEntity>)

    @Query("SELECT * FROM prayer_completions ORDER BY date DESC")
    fun getAllHistory(): Flow<List<PrayerCompletionEntity>>

    @Query("SELECT * FROM prayer_completions WHERE date >= :startEpoch AND date <= :endEpoch ORDER BY date DESC")
    fun getHistoryForDateRange(startEpoch: Long, endEpoch: Long): Flow<List<PrayerCompletionEntity>>

    @Query("SELECT * FROM prayer_completions WHERE id = :id LIMIT 1")
    suspend fun getPrayer(id: String): PrayerCompletionEntity?

    @Query("SELECT COUNT(*) FROM prayer_completions")
    suspend fun getHistoryCount(): Int

    @Query("SELECT DISTINCT date FROM prayer_completions WHERE completed = 1")
    fun getAllCompletedDays(): Flow<List<Long>>
}
