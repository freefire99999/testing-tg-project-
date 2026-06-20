package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

    fun getTelegramToken(): String {
        return prefs.getString("tg_token", "") ?: ""
    }

    fun saveTelegramToken(token: String) {
        prefs.edit().putString("tg_token", token).apply()
    }

    fun getTelegramChatId(): String {
        return prefs.getString("tg_chat_id", "") ?: ""
    }

    fun saveTelegramChatId(chatId: String) {
        prefs.edit().putString("tg_chat_id", chatId).apply()
    }

    fun isForwardingEnabled(): Boolean {
        return prefs.getBoolean("is_forwarding_enabled", true)
    }

    fun setForwardingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_forwarding_enabled", enabled).apply()
    }
}
