package com.pavel.healthmonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pavel.healthmonitor.service.HealthMonitorService

/**
 * Auto-starts [HealthMonitorService] after the device boots.
 *
 * Caveats the README also calls out:
 * - Android 10+ does *not* deliver BOOT_COMPLETED to apps the user
 *   hasn't launched at least once after install. Open MainActivity once
 *   first.
 * - Some OEMs (Xiaomi/MIUI, Oppo/ColorOS, Huawei/EMUI) gate
 *   "auto-start at boot" behind a per-app toggle in their system
 *   settings. Stock Android / Pixel doesn't.
 * - Direct Boot is intentionally not handled. Heart-rate logging while
 *   the user-data partition is still encrypted isn't useful, and
 *   starting a foreground service before user unlock has its own pitfalls.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) {
            Log.w(TAG, "ignoring unexpected action ${intent.action}")
            return
        }
        Log.i(TAG, "boot completed (${intent.action}); starting HealthMonitorService")
        HealthMonitorService.start(context.applicationContext)
    }

    companion object {
        private const val TAG = "HealthBootReceiver"
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
