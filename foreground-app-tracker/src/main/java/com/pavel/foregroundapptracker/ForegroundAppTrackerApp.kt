package com.pavel.foregroundapptracker

import android.app.Application
import com.pavel.foregroundapptracker.data.TrackerDatabase

class ForegroundAppTrackerApp : Application() {

    val database: TrackerDatabase by lazy { TrackerDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    // WorkManager bootstraps itself via the default InitializationProvider
    // declared in its manifest. We don't need a Configuration.Provider here
    // because we don't override the default Configuration.

    companion object {
        @Volatile
        private var instance: ForegroundAppTrackerApp? = null

        fun get(): ForegroundAppTrackerApp =
            checkNotNull(instance) { "ForegroundAppTrackerApp.get() called before onCreate" }
    }
}
