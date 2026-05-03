package com.pavel.healthmonitor

import com.pavel.healthmonitor.data.HeartRateSample
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateSampleTest {

    @Test
    fun `equality is structural`() {
        val a = HeartRateSample(id = 1, bpm = 72, recordedAt = 1_700_000_000_000L, source = "placeholder")
        val b = HeartRateSample(id = 1, bpm = 72, recordedAt = 1_700_000_000_000L, source = "placeholder")
        assertEquals(a, b)
    }
}
