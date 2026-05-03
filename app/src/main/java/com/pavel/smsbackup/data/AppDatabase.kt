package com.pavel.smsbackup.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SmsMessageEntity::class],
    version = 1,
    // Schema export is off for the initial scaffold. Turn this on (and add a
    // `room.schemaLocation` ksp arg in app/build.gradle.kts) before shipping
    // a v2 with migrations so reviewers can see the schema diff.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsMessageDao(): SmsMessageDao

    companion object {
        private const val DB_NAME = "sms_backup.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
