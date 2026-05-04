package com.pavel.inputlogger

import com.pavel.inputlogger.data.EventType
import com.pavel.inputlogger.data.InputEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InputEventTest {

    @Test
    fun `equality is structural`() {
        val a = InputEvent(
            id = 1,
            fieldId = "demo.search",
            eventType = EventType.TEXT_CHANGED.name,
            text = "ab",
            textLength = 2,
            charsDelta = 1,
            timestamp = 1_000L,
        )
        val b = a.copy()
        assertEquals(a, b)
    }

    @Test
    fun `EventType from name returns enum`() {
        assertEquals(EventType.TEXT_CHANGED, EventType.from("TEXT_CHANGED"))
        assertNotNull(EventType.from("FOCUS_GAINED"))
        assertNull(EventType.from("nonsense"))
    }
}
