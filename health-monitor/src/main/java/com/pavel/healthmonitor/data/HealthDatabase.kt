package com.pavel.healthmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HeartRateSample::class],
    version = 1,
    // Set to true and configure schema-export when shipping a v2 with migrations.
    exportSchema = false,
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun heartRateDao(): HeartRateDao

    companion object {
        private const val DB_NAME = "health_monitor.db"

        fun create(context: Context): HealthDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                HealthDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
