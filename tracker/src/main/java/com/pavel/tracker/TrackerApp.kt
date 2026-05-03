package com.pavel.tracker

import android.app.Application
import com.pavel.tracker.data.TrackerDatabase

/**
 * Application class. Owns the singleton [TrackerDatabase] so the listener
 * service and the Activity share the same Room instance.
 */
class TrackerApp : Application() {

    val database: TrackerDatabase by lazy { TrackerDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: TrackerApp? = null

        fun get(): TrackerApp =
            instance ?: error("TrackerApp not yet created")
    }
}
