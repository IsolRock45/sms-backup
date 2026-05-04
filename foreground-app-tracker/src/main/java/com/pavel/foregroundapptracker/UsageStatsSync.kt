package com.pavel.foregroundapptracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.pavel.foregroundapptracker.data.AppUsageDao
import com.pavel.foregroundapptracker.data.AppUsageSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Pulls foreground sessions out of [UsageStatsManager.queryEvents] and writes
 * them to Room.
 *
 * The transformation is straightforward: walk events in time order, when we
 * see `ACTIVITY_RESUMED` for a package, remember the `timeStamp` as the
 * session start; when we see `ACTIVITY_PAUSED` / `ACTIVITY_STOPPED` for the
 * same package, emit a closed session ending at that timestamp. Any session
 * still open at the end of the window is emitted with `endedAt = null`.
 *
 * Re-syncing an overlapping window is idempotent: the DAO uses
 * `OnConflictStrategy.IGNORE` against the unique `(packageName, startedAt)`
 * index, so duplicates from successive syncs are dropped.
 */
class UsageStatsSync(
    private val usm: UsageStatsManager,
    private val dao: AppUsageDao,
    private val packageManager: PackageManager,
) {

    /**
     * Runs a sync over the given window. Returns the number of newly-inserted
     * rows. Rows that close out previously-open sessions are patched in place
     * (counted as `patched` in the log line) and don't add to the return.
     */
    suspend fun sync(beginTimeMillis: Long, endTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        val events = usm.queryEvents(beginTimeMillis, endTimeMillis)
        val open = HashMap<String, Long>()
        val closed = ArrayList<AppUsageSession>()

        val ev = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            when (ev.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // If the same package "resumed" again without a pause, keep the earliest start;
                    // pairing with the next pause produces a single session of the longer span.
                    open.putIfAbsent(pkg, ev.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val started = open.remove(pkg) ?: continue
                    if (ev.timeStamp <= started) continue
                    closed += AppUsageSession(
                        packageName = pkg,
                        appLabel = resolveLabel(pkg),
                        startedAt = started,
                        endedAt = ev.timeStamp,
                        durationMillis = ev.timeStamp - started,
                    )
                }
                else -> Unit
            }
        }
        // Emit any still-open sessions with null end so callers can update them on the next sync.
        val stillOpen = open.map { (pkg, start) ->
            AppUsageSession(
                packageName = pkg,
                appLabel = resolveLabel(pkg),
                startedAt = start,
                endedAt = null,
                durationMillis = null,
            )
        }

        if (closed.isEmpty() && stillOpen.isEmpty()) {
            Log.i(TAG, "sync($beginTimeMillis..$endTimeMillis) produced 0 sessions")
            return@withContext 0
        }

        // A session that was previously emitted as "open" (endedAt=null) and
        // now has its closing event in the current window must be PATCHED,
        // not re-inserted: insertAll uses IGNORE on the (packageName, startedAt)
        // unique index, so naive re-insert would silently drop the closed
        // version and the row would stay permanently open with null duration.
        val existingOpen = dao.openSessions().associateBy { it.packageName to it.startedAt }
        val toInsert = ArrayList<AppUsageSession>(closed.size + stillOpen.size)
        var patched = 0
        for (s in closed) {
            val key = s.packageName to s.startedAt
            val existing = existingOpen[key]
            val endedAt = s.endedAt
            val duration = s.durationMillis
            if (existing != null && existing.endedAt == null && endedAt != null && duration != null) {
                dao.updateEnd(existing.id, endedAt, duration)
                patched++
            } else {
                toInsert += s
            }
        }
        toInsert += stillOpen

        val insertedIds = if (toInsert.isEmpty()) emptyList() else dao.insertAll(toInsert)
        val inserted = insertedIds.count { it >= 0 }
        Log.i(
            TAG,
            "sync window=${endTimeMillis - beginTimeMillis}ms inserted=$inserted patched=$patched skipped=${toInsert.size - inserted}",
        )
        inserted
    }

    private fun resolveLabel(pkg: String): String? = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        private const val TAG = "UsageStatsSync"

        /** Window size for the catch-up sync triggered when the user opens the app. */
        const val DEFAULT_LOOKBACK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

        fun create(context: Context): UsageStatsSync? {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val dao = (context.applicationContext as ForegroundAppTrackerApp)
                .database.appUsageDao()
            return UsageStatsSync(usm, dao, context.packageManager)
        }

        fun sinceLast(now: Long, lastSyncedAt: Long?): Long {
            val since = lastSyncedAt ?: (now - DEFAULT_LOOKBACK_MILLIS)
            // Always look back at least 5min to absorb clock skew + poll jitter.
            return max(0L, since - 5L * 60 * 1000)
        }
    }
}


