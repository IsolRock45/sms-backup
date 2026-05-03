package com.pavel.healthmonitor

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.healthmonitor.data.HealthDatabase
import com.pavel.healthmonitor.data.HeartRateSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartRateDaoTest {

    private lateinit var db: HealthDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_observeRecent_returnsNewestFirst() = runBlocking {
        val dao = db.heartRateDao()
        dao.insert(HeartRateSample(bpm = 70, recordedAt = 1_000L, source = "placeholder"))
        dao.insert(HeartRateSample(bpm = 72, recordedAt = 2_000L, source = "placeholder"))
        dao.insert(HeartRateSample(bpm = 74, recordedAt = 3_000L, source = "placeholder"))

        val rows = dao.observeRecent().first()
        assertEquals(listOf(74, 72, 70), rows.map { it.bpm })
    }
}
