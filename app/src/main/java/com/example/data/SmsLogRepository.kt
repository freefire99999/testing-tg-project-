package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SmsLogRepository(private val smsLogDao: SmsLogDao, context: Context) {
    val allLogs: Flow<List<SmsLog>> = smsLogDao.getAllLogs()
    private val prefs = PreferenceManager(context)

    suspend fun insert(sender: String, body: String, status: String, errorMessage: String? = null): Long {
        val log = SmsLog(sender = sender, body = body, status = status, errorMessage = errorMessage)
        return smsLogDao.insertLog(log)
    }

    suspend fun update(log: SmsLog) {
        smsLogDao.updateLog(log)
    }

    suspend fun clear() {
        smsLogDao.clearAllLogs()
    }

    suspend fun deleteById(id: Int) {
        smsLogDao.deleteLogById(id)
    }

    suspend fun forwardSmsToTelegram(logId: Long, sender: String, body: String) {
        if (!prefs.isForwardingEnabled()) {
            smsLogDao.updateLog(SmsLog(id = logId.toInt(), sender = sender, body = body, status = "DISABLED", errorMessage = "Forwarding is disabled in settings."))
            return
        }

        val token = prefs.getTelegramToken()
        val chatId = prefs.getTelegramChatId()

        if (token.isBlank() || chatId.isBlank()) {
            smsLogDao.updateLog(SmsLog(id = logId.toInt(), sender = sender, body = body, status = "FAILED", errorMessage = "Telegram Token or Chat ID is not configured."))
            return
        }

        val formatMessage = """
            <b>📩 New SMS Received</b>
            <b>📱 From:</b> <code>$sender</code>
            <b>💬 Message:</b>
            $body
        """.trimIndent()

        val result = TelegramService.sendMessage(token, chatId, formatMessage)
        if (result.isSuccess) {
            smsLogDao.updateLog(SmsLog(id = logId.toInt(), sender = sender, body = body, status = "SENT", timestamp = System.currentTimeMillis()))
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            smsLogDao.updateLog(SmsLog(id = logId.toInt(), sender = sender, body = body, status = "FAILED", errorMessage = error, timestamp = System.currentTimeMillis()))
        }
    }
}
