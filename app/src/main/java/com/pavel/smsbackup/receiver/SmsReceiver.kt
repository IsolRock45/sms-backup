package com.pavel.smsbackup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.pavel.smsbackup.SmsBackupApp
import com.pavel.smsbackup.data.SmsMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the system [Telephony.Sms.Intents.SMS_RECEIVED_ACTION] broadcast.
 *
 * Notes on correctness:
 *  - The broadcast can carry multiple [android.telephony.SmsMessage]s when the
 *    text was split into several PDUs. We group them by sender and concatenate
 *    the bodies before persisting, so a long message is one logical row.
 *  - We use [BroadcastReceiver.goAsync] so we can finish a Room write from a
 *    coroutine without the system tearing the receiver down mid-flight.
 *  - We do *not* abort the broadcast — this is a backup app, not the default
 *    SMS handler.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "SMS_RECEIVED with no messages, ignoring")
            return
        }

        // Reassemble multi-part messages: PDUs from the same originating
        // address are pieces of one logical SMS.
        val bySender = messages
            .filter { it.originatingAddress != null }
            .groupBy { it.originatingAddress!! }

        val receivedAt = System.currentTimeMillis()
        val rows = bySender.map { (sender, parts) ->
            SmsMessageEntity(
                sender = sender,
                body = parts.joinToString(separator = "") { it.messageBody.orEmpty() },
                receivedAt = receivedAt,
            )
        }

        // Hand off to a coroutine so the Room insert can suspend without
        // blocking the main thread the system is calling us on.
        val pendingResult = goAsync()
        scope.launch {
            try {
                val dao = SmsBackupApp.get().database.smsMessageDao()
                rows.forEach { dao.insert(it) }
                Log.i(TAG, "Stored ${rows.size} message(s)")
            } catch (t: Throwable) {
                // Swallow, but log — we never want to crash the system process.
                Log.e(TAG, "Failed to persist SMS", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"

        // Receivers are short-lived; a single app-wide scope is fine.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
