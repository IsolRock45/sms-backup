package com.pavel.inputlogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.inputlogger.data.EventType
import com.pavel.inputlogger.data.InputDatabase
import com.pavel.inputlogger.data.InputEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputEventDaoTest {

    private lateinit var db: InputDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            InputDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_observeRecent_returnsNewestFirst() = runBlocking {
        val dao = db.inputEventDao()
        dao.insert(event(fieldId = "a", text = "x", timestamp = 1_000L))
        dao.insert(event(fieldId = "a", text = "xy", timestamp = 2_000L))
        dao.insert(event(fieldId = "b", text = "z", timestamp = 3_000L))

        val rows = dao.observeRecent().first()
        assertEquals(listOf(3_000L, 2_000L, 1_000L), rows.map { it.timestamp })
    }

    @Test
    fun observeFieldHistogram_groupsByField() = runBlocking {
        val dao = db.inputEventDao()
        dao.insert(event(fieldId = "a", text = "1", timestamp = 1_000L))
        dao.insert(event(fieldId = "a", text = "12", timestamp = 2_000L))
        dao.insert(event(fieldId = "b", text = "1", timestamp = 3_000L))

        val hist = dao.observeFieldHistogram(0L).first()
        assertEquals(listOf("a" to 2, "b" to 1), hist.map { it.fieldId to it.eventCount })
    }

    private fun event(fieldId: String, text: String, timestamp: Long) = InputEvent(
        fieldId = fieldId,
        eventType = EventType.TEXT_CHANGED.name,
        text = text,
        textLength = text.length,
        charsDelta = text.length,
        timestamp = timestamp,
    )
}
