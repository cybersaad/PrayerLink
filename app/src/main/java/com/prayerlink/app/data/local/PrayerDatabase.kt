package com.prayerlink.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PrayerCompletionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerCompletionDao(): PrayerCompletionDao
}
