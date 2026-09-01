package com.prayerlink.app.data.local

import androidx.room.TypeConverter
import com.prayerlink.app.data.model.CompletionSource
import com.prayerlink.app.data.model.Prayer
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromPrayer(prayer: Prayer?): String? {
        return prayer?.name
    }

    @TypeConverter
    fun toPrayer(name: String?): Prayer? {
        return name?.let { Prayer.valueOf(it) }
    }

    @TypeConverter
    fun fromCompletionSource(source: CompletionSource?): String? {
        return source?.name
    }

    @TypeConverter
    fun toCompletionSource(name: String?): CompletionSource? {
        return name?.let { CompletionSource.valueOf(it) }
    }
}
