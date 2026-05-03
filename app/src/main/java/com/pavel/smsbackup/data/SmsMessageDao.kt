package com.pavel.smsbackup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageDao {

    @Insert
    suspend fun insert(message: SmsMessageEntity): Long

    @Query("SELECT COUNT(*) FROM sms_messages")
    suspend fun count(): Int

    @Query("SELECT * FROM sms_messages ORDER BY receivedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<SmsMessageEntity>>

    @Query("DELETE FROM sms_messages")
    suspend fun clear()
}
