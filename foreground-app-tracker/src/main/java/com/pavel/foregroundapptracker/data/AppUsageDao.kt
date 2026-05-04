package com.pavel.foregroundapptracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {

    /**
     * IGNORE on the unique `(packageName, startedAt)` index: a periodic
     * sync may re-emit a session that's already in the table; we want
     * the existing row preserved (it may already have an `endedAt`).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sessions: List<AppUsageSession>): List<Long>

    @Query("UPDATE app_usage_sessions SET endedAt = :endedAt, durationMillis = :duration WHERE id = :id")
    suspend fun updateEnd(id: Long, endedAt: Long, duration: Long)

    @Query("SELECT * FROM app_usage_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC")
    suspend fun openSessions(): List<AppUsageSession>

    @Query("SELECT * FROM app_usage_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AppUsageSession>>

    @Query("SELECT COUNT(*) FROM app_usage_sessions")
    suspend fun count(): Int

    @Query(
        """
        SELECT packageName,
               MAX(appLabel) AS appLabel,
               COUNT(*) AS sessionCount,
               COALESCE(SUM(durationMillis), 0) AS totalDurationMillis,
               MAX(startedAt) AS lastUsedAt
        FROM app_usage_sessions
        WHERE startedAt >= :sinceMillis
        GROUP BY packageName
        ORDER BY totalDurationMillis DESC
        """,
    )
    fun observeAggregates(sinceMillis: Long): Flow<List<AppUsageAggregate>>

    @Query("DELETE FROM app_usage_sessions")
    suspend fun clear()
}

data class AppUsageAggregate(
    val packageName: String,
    val appLabel: String?,
    val sessionCount: Int,
    val totalDurationMillis: Long,
    val lastUsedAt: Long,
)
