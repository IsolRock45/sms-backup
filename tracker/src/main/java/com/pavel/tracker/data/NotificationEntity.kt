package com.pavel.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per captured notification.
 *
 * @property id           Auto-incrementing local id.
 * @property packageName  The app that posted the notification (e.g.
 *                        `com.whatsapp`). Reliable; comes straight from
 *                        the system.
 * @property title        `Notification.EXTRA_TITLE` if present, else null.
 * @property text         `Notification.EXTRA_TEXT` (or `EXTRA_BIG_TEXT` /
 *                        `EXTRA_SUMMARY_TEXT` as fallbacks). Stored as a
 *                        plain String — `CharSequence` styling is dropped
 *                        on purpose.
 * @property postedAt     `StatusBarNotification.postTime` — when the source
 *                        app told the system to post the notification.
 * @property capturedAt   `System.currentTimeMillis()` at the moment we
 *                        observed it. Useful when comparing wall clock vs
 *                        the source app's clock (e.g. backfill / replay).
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["postedAt"]),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val capturedAt: Long,
)
