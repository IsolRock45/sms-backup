package com.pavel.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Singleton entry point for posting heads-up reminder notifications.
 *
 * Designed to be called from anywhere in the app (Activity, ViewModel,
 * Worker, AlarmReceiver, etc.):
 *
 * ```kotlin
 * ReminderNotifier.show(context, "Take a break", "10 minutes of stretching")
 * ```
 *
 * The notification channel is created on first call (idempotent), and
 * the auto-incrementing notification id is returned so callers can later
 * `cancel()` a specific reminder if needed.
 */
object ReminderNotifier {

    /**
     * The user-facing channel ID. Bumping this string is the only way to
     * change channel-level settings (importance, sound) once a user has
     * touched the channel in Settings — Android intentionally ignores
     * subsequent changes to a channel of the same id.
     */
    const val CHANNEL_ID = "reminders.high"

    private const val CHANNEL_NAME = "Reminders"
    private const val CHANNEL_DESC = "Heads-up reminders for tasks and timers"

    /** Auto-incrementing id source so concurrent reminders don't overwrite each other. */
    private val nextId = AtomicInteger(1)

    /**
     * Show a heads-up reminder notification.
     *
     * @return the notification id, or `null` if posting was suppressed because
     *         the app does not hold `POST_NOTIFICATIONS` (Android 13+).
     *         The caller can decide whether to surface a fallback UI.
     */
    // Lint can't trace the inline `hasPostPermission` guard through the
    // NotificationManagerCompat.notify call — but we always check before
    // calling, so suppress at the function boundary.
    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        title: String,
        message: String,
        contentIntent: PendingIntent? = defaultContentIntent(context),
    ): Int? {
        val appContext = context.applicationContext

        if (!hasPostPermission(appContext)) return null

        ensureChannel(appContext)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder_star)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            // Heads-up display: importance HIGH on the channel + priority
            // HIGH on the builder. Both are needed because pre-O devices
            // ignore the channel and post-O devices ignore the priority.
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // `CATEGORY_REMINDER` lets the OS rank this above non-urgent
            // notifications when DND filters allow "Reminders only".
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Default sound + vibration so it actually pops up on lockscreen.
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val id = nextId.getAndIncrement()
        NotificationManagerCompat.from(appContext).notify(id, notification)
        return id
    }

    /** Dismiss a previously shown reminder. Safe to call with an unknown id. */
    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context.applicationContext).cancel(notificationId)
    }

    /**
     * Channels are immutable after first creation w.r.t. importance/sound,
     * but `createNotificationChannel` itself is idempotent — calling it
     * every time `show(...)` runs is fine and keeps callers boilerplate-free.
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            setShowBadge(true)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun hasPostPermission(context: Context): Boolean {
        // POST_NOTIFICATIONS is only enforced on API 33+. Below that, apps
        // hold it implicitly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Default tap action: bring the launcher Activity back to the front.
     * Callers can pass their own `PendingIntent` to deep-link into a
     * specific screen instead.
     */
    private fun defaultContentIntent(context: Context): PendingIntent? {
        val pm = context.packageManager
        val launch = pm.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        // FLAG_IMMUTABLE is required on API 31+; combining with UPDATE_CURRENT
        // means a re-issued reminder reuses the same PendingIntent slot
        // instead of leaking entries.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, launch, flags)
    }
}
