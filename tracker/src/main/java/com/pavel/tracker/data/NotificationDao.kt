package com.pavel.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT packageName, COUNT(*) AS count
        FROM notifications
        WHERE postedAt >= :sinceMillis
        GROUP BY packageName
        ORDER BY count DESC
        """,
    )
    fun observePackageHistogram(sinceMillis: Long): Flow<List<PackageCount>>

    @Query("DELETE FROM notifications")
    suspend fun clear()
}

/** Aggregate row for the "what's been buzzing me" view. */
data class PackageCount(
    val packageName: String,
    val count: Int,
)
