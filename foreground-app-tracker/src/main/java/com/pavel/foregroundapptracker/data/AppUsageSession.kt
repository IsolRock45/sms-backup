package com.pavel.foregroundapptracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One foreground session of one app: from `ACTIVITY_RESUMED` until the
 * next `ACTIVITY_PAUSED` (or `ACTIVITY_STOPPED`) in the
 * `UsageStatsManager.queryEvents()` stream.
 *
 * `(packageName, startedAt)` is unique by construction (a single app can't
 * be resumed twice at the same wall-clock millisecond), so we use it as a
 * deduplication key when re-syncing overlapping windows.
 */
@Entity(
    tableName = "app_usage_sessions",
    indices = [
        Index(value = ["packageName", "startedAt"], unique = true),
        Index(value = ["startedAt"]),
    ],
)
data class AppUsageSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    /** Best-effort label resolved at sync time via PackageManager; may be null if app is uninstalled. */
    val appLabel: String?,
    /** `Event.timeStamp` of the `ACTIVITY_RESUMED`. */
    val startedAt: Long,
    /** `Event.timeStamp` of the matching pause/stop, or null if the session was still open at sync time. */
    val endedAt: Long?,
    /** `endedAt - startedAt` if both are known; otherwise null. Stored denormalized for cheap aggregate queries. */
    val durationMillis: Long?,
)
