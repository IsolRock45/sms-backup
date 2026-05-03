package com.pavel.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    // Same rationale as the SMS module: turn this on (and add a
    // `room.schemaLocation` ksp arg) before shipping a v2 with migrations.
    exportSchema = false,
)
abstract class TrackerDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val DB_NAME = "tracker.db"

        @Volatile
        private var instance: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): TrackerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TrackerDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
