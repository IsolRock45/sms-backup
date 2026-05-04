package com.pavel.foregroundapptracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.foregroundapptracker.data.AppUsageSession
import com.pavel.foregroundapptracker.data.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUsageDaoTest {

    private lateinit var db: TrackerDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrackerDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAll_dedupesUniquePackageStartedAt() = runBlocking {
        val dao = db.appUsageDao()
        val s1 = session("com.a", 1_000L, 2_000L)
        val s1Dup = session("com.a", 1_000L, 9_999L) // same key → IGNORE
        val s2 = session("com.a", 3_000L, 4_000L)
        dao.insertAll(listOf(s1, s1Dup, s2))

        val rows = dao.observeRecent().first()
        assertEquals(2, rows.size)
    }

    @Test
    fun updateEnd_patchesOpenSessionToClosed() = runBlocking {
        val dao = db.appUsageDao()
        // Simulate the "still open at sync boundary" state: a session inserted
        // with endedAt=null and durationMillis=null, as UsageStatsSync emits.
        dao.insertAll(listOf(session("com.x", startedAt = 1_000L, endedAt = null)))
        val openRows = dao.openSessions()
        assertEquals(1, openRows.size)
        val open = openRows.single()
        assertEquals(null, open.endedAt)
        assertEquals(null, open.durationMillis)

        // The next sync sees both RESUMED and PAUSED for the same session.
        // Re-inserting via insertAll is dropped by IGNORE; updateEnd is the
        // only correct path.
        dao.updateEnd(open.id, endedAt = 5_000L, duration = 4_000L)

        val recent = dao.observeRecent().first()
        assertEquals(1, recent.size)
        val patched = recent.single()
        assertEquals(open.id, patched.id)
        assertEquals(5_000L, patched.endedAt)
        assertEquals(4_000L, patched.durationMillis)
        assertEquals(emptyList<AppUsageSession>(), dao.openSessions())
    }

    @Test
    fun observeAggregates_sumsDurationsByPackage() = runBlocking {
        val dao = db.appUsageDao()
        dao.insertAll(
            listOf(
                session("com.a", 1_000L, 3_000L),
                session("com.a", 4_000L, 7_000L),
                session("com.b", 2_000L, 8_000L),
            ),
        )

        val agg = dao.observeAggregates(0L).first()
        assertEquals(2, agg.size)
        // ORDER BY totalDurationMillis DESC: com.b=6000, com.a=2000+3000=5000.
        assertEquals("com.b", agg[0].packageName)
        assertEquals(6_000L, agg[0].totalDurationMillis)
        assertEquals("com.a", agg[1].packageName)
        assertEquals(5_000L, agg[1].totalDurationMillis)
        assertEquals(2, agg[1].sessionCount)
    }

    private fun session(pkg: String, start: Long, end: Long?) = AppUsageSession(
        packageName = pkg,
        appLabel = pkg,
        startedAt = start,
        endedAt = end,
        durationMillis = end?.let { it - start },
    )
}
