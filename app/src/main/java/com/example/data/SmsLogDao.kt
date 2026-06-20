package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLog): Long

    @Update
    suspend fun updateLog(log: SmsLog)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}
