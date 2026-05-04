package com.pavel.foregroundapptracker

import com.pavel.foregroundapptracker.UsageStatsSync.Companion.sinceLast
import com.pavel.foregroundapptracker.data.AppUsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageSessionTest {

    @Test
    fun `equality is structural`() {
        val a = AppUsageSession(
            id = 1,
            packageName = "com.example",
            appLabel = "Example",
            startedAt = 1_000L,
            endedAt = 2_000L,
            durationMillis = 1_000L,
        )
        assertEquals(a, a.copy())
    }

    @Test
    fun `sinceLast falls back to default lookback when null`() {
        val now = 1_000_000_000L
        val expected = now - UsageStatsSync.DEFAULT_LOOKBACK_MILLIS - 5L * 60 * 1000
        assertEquals(expected, sinceLast(now, lastSyncedAt = null))
    }

    @Test
    fun `sinceLast subtracts safety window from last`() {
        val last = 1_000_000_000L
        val result = sinceLast(now = last + 60_000, lastSyncedAt = last)
        assertEquals(last - 5L * 60 * 1000, result)
    }

    @Test
    fun `sinceLast clamps to zero rather than going negative`() {
        // Synthetic: last=0, now=0 → should not produce a negative window start.
        assertTrue(sinceLast(now = 0L, lastSyncedAt = 0L) >= 0L)
    }
}
