package com.pavel.healthmonitor

import android.app.Application
import com.pavel.healthmonitor.data.HealthDatabase

/**
 * Application class. Exposes the singleton [HealthDatabase] for the
 * service and Activity to share. Idiomatic for a small one-DB project;
 * swap for Hilt / Koin if the dependency graph grows.
 */
class HealthMonitorApp : Application() {

    val database: HealthDatabase by lazy { HealthDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: HealthMonitorApp? = null

        fun get(): HealthMonitorApp =
            checkNotNull(instance) { "HealthMonitorApp.get() called before onCreate" }
    }
}
