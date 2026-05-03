package com.pavel.healthmonitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {

    @Insert
    suspend fun insert(sample: HeartRateSample): Long

    @Query("SELECT COUNT(*) FROM heart_rate_samples")
    suspend fun count(): Int

    @Query("SELECT * FROM heart_rate_samples ORDER BY recordedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<HeartRateSample>>

    @Query("SELECT * FROM heart_rate_samples ORDER BY recordedAt DESC LIMIT 1")
    suspend fun latest(): HeartRateSample?

    @Query("DELETE FROM heart_rate_samples")
    suspend fun clear()
}
