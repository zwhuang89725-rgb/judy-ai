package com.pixelbuddy.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 使用 EncryptedSharedPreferences 安全存储 API Key。
 *
 * 数据使用 AES-256 GCM 加密，主密钥由 Android Keystore 管理。
 * 与 Room 中的 ModelConfig 配合使用 — Room 存非敏感字段，此 Store 存密文。
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "pixel_buddy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Chat API Key ──
    var chatApiKey: String
        get() = prefs.getString(KEY_CHAT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHAT_API_KEY, value).apply()

    // ── STT API Key ──
    var sttApiKey: String
        get() = prefs.getString(KEY_STT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STT_API_KEY, value).apply()

    // ── TTS API Key ──
    var ttsApiKey: String
        get() = prefs.getString(KEY_TTS_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TTS_API_KEY, value).apply()

    /** 清除所有 Key */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private companion object {
        private const val KEY_CHAT_API_KEY = "chat_api_key"
        private const val KEY_STT_API_KEY = "stt_api_key"
        private const val KEY_TTS_API_KEY = "tts_api_key"
    }
}
