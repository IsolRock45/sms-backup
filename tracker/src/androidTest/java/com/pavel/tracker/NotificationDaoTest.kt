package com.pavel.tracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.tracker.data.NotificationDao
import com.pavel.tracker.data.NotificationEntity
import com.pavel.tracker.data.TrackerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var db: TrackerDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrackerDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.notificationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_then_observeRecent_returns_inserted_row() = runBlocking {
        val row = NotificationEntity(
            packageName = "com.whatsapp",
            title = "Alice",
            text = "hi",
            postedAt = 42L,
            capturedAt = 50L,
        )
        dao.insert(row)

        val recent = dao.observeRecent(limit = 10).first()
        assertEquals(1, recent.size)
        assertEquals("Alice", recent[0].title)
        assertEquals("com.whatsapp", recent[0].packageName)
    }

    @Test
    fun observeRecent_orders_by_postedAt_desc() = runBlocking {
        dao.insert(
            NotificationEntity(
                packageName = "a", title = null, text = "old", postedAt = 1L, capturedAt = 1L,
            ),
        )
        dao.insert(
            NotificationEntity(
                packageName = "b", title = null, text = "new", postedAt = 2L, capturedAt = 2L,
            ),
        )

        val recent = dao.observeRecent(limit = 10).first()
        assertEquals(listOf("new", "old"), recent.map { it.text })
    }

    @Test
    fun packageHistogram_groups_by_package_after_since() = runBlocking {
        dao.insert(NotificationEntity(packageName = "a", title = null, text = "1", postedAt = 5L, capturedAt = 5L))
        dao.insert(NotificationEntity(packageName = "a", title = null, text = "2", postedAt = 6L, capturedAt = 6L))
        dao.insert(NotificationEntity(packageName = "b", title = null, text = "3", postedAt = 7L, capturedAt = 7L))
        dao.insert(NotificationEntity(packageName = "c", title = null, text = "old", postedAt = 1L, capturedAt = 1L))

        val histogram = dao.observePackageHistogram(sinceMillis = 4L).first()
        assertEquals(listOf("a", "b"), histogram.map { it.packageName })
        assertEquals(listOf(2, 1), histogram.map { it.count })
    }
}
