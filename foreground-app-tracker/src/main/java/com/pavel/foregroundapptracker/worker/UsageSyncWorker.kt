package com.pavel.foregroundapptracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pavel.foregroundapptracker.UsageAccess
import com.pavel.foregroundapptracker.UsageStatsSync
import java.util.concurrent.TimeUnit

class UsageSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!UsageAccess.isGranted(applicationContext)) {
            // Permission was revoked; nothing to do. Return success so the
            // worker stays scheduled and resumes on its own once the user
            // re-enables Usage Access.
            return Result.success()
        }
        val sync = UsageStatsSync.create(applicationContext) ?: return Result.failure()
        val now = System.currentTimeMillis()
        // 30min window covers the 15min worker cadence + slack for missed runs.
        val begin = now - WINDOW_MILLIS
        return runCatching { sync.sync(begin, now) }
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "foreground-app-tracker.periodic-sync"
        private const val INTERVAL_MINUTES = 15L
        private val WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(30)

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
