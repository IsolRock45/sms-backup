package com.pavel.smsbackup

import com.pavel.smsbackup.data.SmsMessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM tests over the data class. Anything that touches Room or the
 * Android framework lives in androidTest.
 */
class SmsMessageEntityTest {

    @Test
    fun `entity equality respects all fields`() {
        val a = SmsMessageEntity(id = 1, sender = "+1", body = "hi", receivedAt = 100L)
        val b = SmsMessageEntity(id = 1, sender = "+1", body = "hi", receivedAt = 100L)
        val c = a.copy(body = "bye")

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `default id is zero so Room autogenerates it on insert`() {
        val row = SmsMessageEntity(sender = "+1", body = "hi", receivedAt = 0L)
        assertEquals(0L, row.id)
    }
}
