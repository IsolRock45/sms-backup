package com.pavel.tracker.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pavel.tracker.TrackerApp
import com.pavel.tracker.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures every notification posted on the device while notification
 * access is granted, and persists a row per "interesting" notification.
 *
 * The system manages the lifecycle of this service — `onCreate` /
 * `onListenerConnected` are called when the user grants access in
 * Settings, and `onListenerDisconnected` / `onDestroy` when access is
 * revoked or the OS rebinds. We do not need (and must not have) a
 * `startService`/`bindService` from our own code.
 */
class NotificationCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "listener disconnected")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldCapture(sbn)) return

        val extras = sbn.notification.extras
        val title = extras.bestTitle()
        val text = extras.bestText()

        // Drop notifications that have neither a title nor any text — they
        // are almost always system placeholders (sync glyphs, RCS chips,
        // etc) and are not useful for productivity analysis.
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val entity = NotificationEntity(
            packageName = sbn.packageName,
            title = title,
            text = text,
            postedAt = sbn.postTime,
            capturedAt = System.currentTimeMillis(),
        )

        scope.launch {
            try {
                TrackerApp.get().database.notificationDao().insert(entity)
            } catch (t: Throwable) {
                // Never crash the system_server-bound service.
                Log.e(TAG, "Failed to persist notification from ${sbn.packageName}", t)
            }
        }
    }

    private fun shouldCapture(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags

        // Group summary entries duplicate their children — skip them so a
        // single chat thread doesn't get double-counted.
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false

        // Foreground-service notifications (the persistent "Spotify is
        // playing", "Maps is navigating") are noise for usage analysis.
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false

        // Skip ongoing notifications that the user can't dismiss — same
        // reason: they describe device state, not "events".
        if (sbn.isOngoing) return false

        // Don't capture our own UI; harmless but pointless.
        if (sbn.packageName == packageName) return false

        return true
    }

    companion object {
        private const val TAG = "NotifCaptureSvc"
    }
}

/**
 * Tries `EXTRA_TITLE`, then `EXTRA_TITLE_BIG`, both of which may be
 * `CharSequence` (e.g. `SpannableString`).
 */
private fun Bundle.bestTitle(): String? =
    getCharSequence(Notification.EXTRA_TITLE)?.toString()
        ?: getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()

/**
 * Tries `EXTRA_BIG_TEXT` (BigTextStyle), `EXTRA_TEXT`, then
 * `EXTRA_SUMMARY_TEXT`. `EXTRA_TEXT_LINES` (InboxStyle) is intentionally
 * not joined — single inbox notifications repeat in `EXTRA_TEXT`.
 */
private fun Bundle.bestText(): String? =
    getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        ?: getCharSequence(Notification.EXTRA_TEXT)?.toString()
        ?: getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
