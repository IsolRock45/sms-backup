package com.pavel.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests over the small helpers that don't need a Context.
 * Anything that touches `NotificationManager` lives in androidTest.
 */
class ReminderNotifierTest {

    @Test
    fun `channel id is stable - bump only on importance changes`() {
        // Tripwire test: if you change CHANNEL_ID, you also need to migrate
        // any pre-existing channel because Android ignores subsequent
        // importance changes on the original channel.
        assertEquals("reminders.high", ReminderNotifier.CHANNEL_ID)
    }
}
