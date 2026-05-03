package com.pavel.tracker

import com.pavel.tracker.data.NotificationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM tests over the data class. Anything that touches Room or the
 * Android framework lives in androidTest.
 */
class NotificationEntityTest {

    @Test
    fun `entity equality respects all fields`() {
        val a = NotificationEntity(
            id = 1,
            packageName = "com.whatsapp",
            title = "Alice",
            text = "hi",
            postedAt = 100L,
            capturedAt = 110L,
        )
        val b = a.copy()
        val c = a.copy(text = "bye")

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `default id is zero so Room autogenerates it on insert`() {
        val row = NotificationEntity(
            packageName = "com.whatsapp",
            title = null,
            text = "hi",
            postedAt = 0L,
            capturedAt = 0L,
        )
        assertEquals(0L, row.id)
    }
}
