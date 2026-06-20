package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SENT", "FAILED", "DISABLED", "PENDING"
    val errorMessage: String? = null
)
