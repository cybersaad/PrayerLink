package com.prayerlink.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prayerlink.app.data.model.CompletionSource
import com.prayerlink.app.data.model.Prayer
import java.time.LocalDate

@Entity(tableName = "prayer_completions")
data class PrayerCompletionEntity(
    @PrimaryKey
    val id: String, // format: "2026-07-23_FAJR"

    val date: LocalDate,

    val prayer: Prayer,

    val completed: Boolean,

    val completedAt: Long?, // Null for legacy records

    val reminderAnswered: Boolean,

    val source: CompletionSource,

    val createdAt: Long,

    val updatedAt: Long
)
