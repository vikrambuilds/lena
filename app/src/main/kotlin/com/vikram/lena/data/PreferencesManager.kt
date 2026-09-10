package com.vikram.lena.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lena_prefs", Context.MODE_PRIVATE)

    companion object {
        // Keys
        const val KEY_GEMINI_API = "gemini_key"
        const val KEY_OPENAI_API = "openai_key"
        const val KEY_AUTO_START = "auto_start"
        const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_TTS_PITCH = "tts_pitch"
        const val KEY_LANGUAGE = "language"
        const val KEY_PROACTIVE_GREETINGS = "proactive_greetings"
        const val KEY_SAVE_CONVERSATIONS = "save_conversations"
        const val KEY_NOISE_CANCELLATION = "noise_cancellation"
        const val KEY_PREFERRED_AI = "preferred_ai"
        const val KEY_USER_NAME = "user_name"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_SERVICE_RUNNING = "service_running"
    }

    // ========== API KEYS ==========

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API, "") ?: ""
        set(value) = prefs.edit { putString(KEY_GEMINI_API, value) }

    var openaiApiKey: String
        get() = prefs.getString(KEY_OPENAI_API, "") ?: ""
        set(value) = prefs.edit { putString(KEY_OPENAI_API, value) }

    // ========== SERVICE SETTINGS ==========

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START, value) }

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_WAKE_WORD_ENABLED, value) }

    var serviceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_RUNNING, value) }

    // ========== VOICE SETTINGS ==========

    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 0.92f)
        set(value) = prefs.edit { putFloat(KEY_TTS_SPEED, value) }

    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.05f)
        set(value) = prefs.edit { putFloat(KEY_TTS_PITCH, value) }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "hi-IN") ?: "hi-IN"
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    // ========== FEATURE TOGGLES ==========

    var proactiveGreetings: Boolean
        get() = prefs.getBoolean(KEY_PROACTIVE_GREETINGS, true)
        set(value) = prefs.edit { putBoolean(KEY_PROACTIVE_GREETINGS, value) }

    var saveConversations: Boolean
        get() = prefs.getBoolean(KEY_SAVE_CONVERSATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_SAVE_CONVERSATIONS, value) }

    var noiseCancellation: Boolean
        get() = prefs.getBoolean(KEY_NOISE_CANCELLATION, true)
        set(value) = prefs.edit { putBoolean(KEY_NOISE_CANCELLATION, value) }

    // ========== AI SETTINGS ==========

    var preferredAI: String
        get() = prefs.getString(KEY_PREFERRED_AI, "auto") ?: "auto"
        set(value) = prefs.edit { putString(KEY_PREFERRED_AI, value) }

    // ========== USER SETTINGS ==========

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Vikram") ?: "Vikram"
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_RUN, value) }

    // ========== UTILITY ==========

    fun hasApiKeys(): Boolean {
        return geminiApiKey.isNotBlank()
    }

    fun clearAll() {
        prefs.edit { clear() }
    }
}