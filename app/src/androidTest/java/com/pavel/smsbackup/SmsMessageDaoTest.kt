package com.pavel.smsbackup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavel.smsbackup.data.AppDatabase
import com.pavel.smsbackup.data.SmsMessageDao
import com.pavel.smsbackup.data.SmsMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsMessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SmsMessageDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.smsMessageDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_then_observeRecent_returns_inserted_row() = runBlocking {
        val row = SmsMessageEntity(sender = "+15551234567", body = "hello", receivedAt = 42L)
        dao.insert(row)

        val recent = dao.observeRecent(limit = 10).first()
        assertEquals(1, recent.size)
        assertEquals("hello", recent[0].body)
        assertEquals("+15551234567", recent[0].sender)
        assertEquals(42L, recent[0].receivedAt)
    }

    @Test
    fun observeRecent_orders_by_receivedAt_desc() = runBlocking {
        dao.insert(SmsMessageEntity(sender = "a", body = "old", receivedAt = 1L))
        dao.insert(SmsMessageEntity(sender = "b", body = "new", receivedAt = 2L))

        val recent = dao.observeRecent(limit = 10).first()
        assertEquals(listOf("new", "old"), recent.map { it.body })
    }
}
