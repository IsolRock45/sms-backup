package com.pavel.inputlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InputEvent::class],
    version = 1,
    // Set true and configure schema-export when shipping a v2 with migrations.
    exportSchema = false,
)
abstract class InputDatabase : RoomDatabase() {

    abstract fun inputEventDao(): InputEventDao

    companion object {
        private const val DB_NAME = "input_logger.db"

        fun create(context: Context): InputDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                InputDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
