package com.pavel.foregroundapptracker

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * `PACKAGE_USAGE_STATS` is gated by an `appop`. The standard way to check
 * whether the user has granted it from Settings is via
 * [AppOpsManager.checkOpNoThrow] with `OPSTR_GET_USAGE_STATS`.
 *
 * `AppOpsManager.unsafeCheckOpNoThrow` is the API 29+ replacement of the
 * deprecated `checkOpNoThrow`; we branch on the SDK level so older devices
 * still work.
 */
object UsageAccess {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
