package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferenceManager
import com.example.data.SmsLog
import com.example.data.SmsLogRepository
import com.example.data.TelegramService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = SmsLogRepository(db.smsLogDao(), application)
    private val preferenceManager = PreferenceManager(application)

    // Reactive flow of SMS logs from the database
    val logsState: StateFlow<List<SmsLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Config entries matching our fields to user preference states
    private val _token = MutableStateFlow(preferenceManager.getTelegramToken())
    val token = _token.asStateFlow()

    private val _chatId = MutableStateFlow(preferenceManager.getTelegramChatId())
    val chatId = _chatId.asStateFlow()

    private val _isForwardingEnabled = MutableStateFlow(preferenceManager.isForwardingEnabled())
    val isForwardingEnabled = _isForwardingEnabled.asStateFlow()

    // Bot connection verification states
    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus = _testStatus.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    fun updateToken(newValue: String) {
        _token.value = newValue
        preferenceManager.saveTelegramToken(newValue)
    }

    fun updateChatId(newValue: String) {
        _chatId.value = newValue
        preferenceManager.saveTelegramChatId(newValue)
    }

    fun toggleForwarding(enabled: Boolean) {
        _isForwardingEnabled.value = enabled
        preferenceManager.setForwardingEnabled(enabled)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun retryForward(log: SmsLog) {
        viewModelScope.launch {
            // Re-mark as PENDING and retry forwarding
            repository.update(log.copy(status = "PENDING", errorMessage = null))
            repository.forwardSmsToTelegram(log.id.toLong(), log.sender, log.body)
        }
    }

    fun triggerTestMessage() {
        val currentToken = token.value.trim()
        val currentChatId = chatId.value.trim()

        if (currentToken.isBlank() || currentChatId.isBlank()) {
            _testStatus.value = "Error: Please input both Telegram Bot Token and Chat ID."
            return
        }

        viewModelScope.launch {
            _isTesting.value = true
            _testStatus.value = "Sending test message..."
            
            val formattedTime = java.text.SimpleDateFormat(
                "dd MMM yyyy, hh:mm:ss a", 
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            val testMsg = """
                <b>🔔 SMS Forwarder Connection Test</b>
                
                🎉 Congratulations! Your Android SMS to Telegram Forwarder connection test was fully successful.
                
                🕒 <b>Time:</b> $formattedTime
                🛠 <b>Status:</b> Active & Listening
            """.trimIndent()
            
            val result = TelegramService.sendMessage(currentToken, currentChatId, testMsg)
            _isTesting.value = false
            if (result.isSuccess) {
                _testStatus.value = "Success! Test message delivered successfully."
            } else {
                _testStatus.value = "Failed: ${result.exceptionOrNull()?.message ?: "Unknown connection error"}"
            }
        }
    }

    fun clearTestStatus() {
        _testStatus.value = null
    }
}
