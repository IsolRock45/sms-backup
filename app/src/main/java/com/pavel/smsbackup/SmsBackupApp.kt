package com.pavel.smsbackup

import android.app.Application
import com.pavel.smsbackup.data.AppDatabase

/**
 * Application class. Owns the singleton [AppDatabase] so the broadcast receiver
 * and the Activity share the same Room instance and end up writing to the same
 * SQLite file.
 */
class SmsBackupApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: SmsBackupApp? = null

        fun get(): SmsBackupApp =
            instance ?: error("SmsBackupApp not yet created")
    }
}
