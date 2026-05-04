package com.pavel.inputlogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InputEventDao {

    @Insert
    suspend fun insert(event: InputEvent): Long

    @Query("SELECT COUNT(*) FROM input_events")
    suspend fun count(): Int

    @Query("SELECT * FROM input_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<InputEvent>>

    @Query(
        """
        SELECT fieldId, COUNT(*) AS eventCount
        FROM input_events
        WHERE timestamp >= :sinceMillis
        GROUP BY fieldId
        ORDER BY eventCount DESC
        """,
    )
    fun observeFieldHistogram(sinceMillis: Long): Flow<List<FieldEventCount>>

    @Query("DELETE FROM input_events")
    suspend fun clear()
}

data class FieldEventCount(
    val fieldId: String,
    val eventCount: Int,
)
