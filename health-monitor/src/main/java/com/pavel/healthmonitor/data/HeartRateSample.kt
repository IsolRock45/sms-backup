package com.pavel.healthmonitor.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "heart_rate_samples",
    indices = [Index(value = ["recordedAt"])],
)
data class HeartRateSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Beats per minute as reported by the sensor / placeholder source. */
    val bpm: Int,
    /** Wall-clock time the sample was recorded. */
    val recordedAt: Long,
    /** Source of the reading — useful when mixing real and placeholder data. */
    val source: String,
)
