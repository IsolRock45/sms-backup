package com.pavel.smsbackup.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per received SMS.
 *
 * @property id        Auto-incrementing local id.
 * @property sender    The originating phone number, exactly as reported by the
 *                     telephony stack (could be E.164, short code, alphanumeric
 *                     sender id, etc.).
 * @property body      The full message body. Multi-part SMSes are joined into
 *                     a single string before being persisted.
 * @property receivedAt Unix epoch millis at the moment the broadcast was
 *                     received on this device. We deliberately do *not* trust
 *                     `SmsMessage.timestampMillis` because it is the SMSC
 *                     timestamp and can be wildly off.
 */
@Entity(
    tableName = "sms_messages",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["receivedAt"]),
    ],
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
)
