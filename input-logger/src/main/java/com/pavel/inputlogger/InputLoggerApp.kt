package com.pavel.inputlogger

import android.app.Application
import com.pavel.inputlogger.data.InputDatabase

/**
 * Application class. Exposes the singleton database for the Activity
 * (and any future ViewModels) to share.
 */
class InputLoggerApp : Application() {

    val database: InputDatabase by lazy { InputDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: InputLoggerApp? = null

        fun get(): InputLoggerApp =
            checkNotNull(instance) { "InputLoggerApp.get() called before onCreate" }
    }
}
