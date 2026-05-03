package com.pavel.healthmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pavel.healthmonitor.HealthMonitorApp
import com.pavel.healthmonitor.MainActivity
import com.pavel.healthmonitor.R
import com.pavel.healthmonitor.data.HeartRateSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Foreground service that runs while the app is "monitoring vitals".
 *
 * Lifecycle:
 * - Started by [MainActivity] (user tap) or [com.pavel.healthmonitor.receiver.BootCompletedReceiver]
 *   (after device reboot, *if* the user has launched the app at least
 *   once on Android 10+).
 * - Calls [startForeground] within a few seconds of [onStartCommand] —
 *   required by the OS or the system kills the process with `RemoteServiceException`.
 * - Returns [START_STICKY] so the OS recreates the service after a
 *   low-memory kill (with a `null` Intent on the recreate).
 *
 * Sampling source:
 * - Real wearable / chest-strap integration belongs in a separate sensor
 *   layer. Phones don't ship `Sensor.TYPE_HEART_RATE`, and the right
 *   real source depends on the user's hardware (BLE GATT 0x2A37, Health
 *   Connect, Wear OS data layer).
 * - For now this emits a placeholder reading every 5 seconds so the
 *   end-to-end persistence + UI flow is exercised. The placeholder
 *   readings are tagged `source = "placeholder"` in the database so
 *   they're trivially filterable / deletable when a real source is
 *   wired in.
 */
class HealthMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var samplingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        Log.i(TAG, "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        startSampling()
        // START_STICKY: system recreates the service after a kill with
        // a null Intent. We do NOT use START_REDELIVER_INTENT because
        // we don't carry per-start payload that needs replaying.
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "service destroyed")
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: the foregroundServiceType passed here must
            // match the one declared in the manifest.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(tapPending)
            .setOngoing(true)
            // Low priority: this is a *status* notification, not an
            // alert. Users see it in the shade but it doesn't pop.
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startSampling() {
        if (samplingJob?.isActive == true) return
        samplingJob = scope.launch {
            val dao = HealthMonitorApp.get().database.heartRateDao()
            while (isActive) {
                val sample = HeartRateSample(
                    bpm = placeholderBpm(),
                    recordedAt = System.currentTimeMillis(),
                    source = "placeholder",
                )
                runCatching { dao.insert(sample) }
                    .onFailure { Log.e(TAG, "failed to persist sample", it) }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * Stand-in until a real sensor source is wired in. Picks a value in
     * resting → light-activity range with small per-step jitter so the
     * UI graph looks like sensor data, not a flat line.
     */
    private fun placeholderBpm(): Int {
        val base = 72
        val jitter = Random.nextInt(-6, 7)
        return (base + jitter).coerceIn(40, 200)
    }

    companion object {
        private const val TAG = "HealthMonitorSvc"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "health.monitor.status"
        private const val CHANNEL_NAME = "Continuous monitoring"
        private const val CHANNEL_DESC = "Persistent notification while vitals monitoring is running."

        // 5 s is a pragmatic placeholder cadence. Real sources have
        // their own cadence — Health Connect events, BLE notifications,
        // Wear OS data layer pushes — and would replace this loop entirely.
        private const val SAMPLE_INTERVAL_MS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, HealthMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HealthMonitorService::class.java))
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
