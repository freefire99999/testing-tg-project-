package com.example.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object TelegramService {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(token: String, chatId: String, text: String): Result<String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (token.isBlank() || chatId.isBlank()) {
                    return@withContext Result.failure(Exception("Telegram Bot Token or Chat ID is blank. Please configure them in setup."))
                }
                val url = "https://api.telegram.org/bot$token/sendMessage"
                
                val jsonBody = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", text)
                    put("parse_mode", "HTML")
                }.toString()

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        Result.success(responseStr)
                    } else {
                        val telegramError = try {
                            JSONObject(responseStr).getString("description")
                        } catch (e: Exception) {
                            "Error code: ${response.code}"
                        }
                        Result.failure(IOException("Telegram API error: $telegramError"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
