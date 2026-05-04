package com.pavel.foregroundapptracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppUsageSession::class],
    version = 1,
    exportSchema = false,
)
abstract class TrackerDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao

    companion object {
        private const val DB_NAME = "foreground_app_tracker.db"

        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TrackerDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
