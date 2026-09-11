package com.vikram.lena.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lena_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_key", "") ?: ""
        set(value) = prefs.edit { putString("gemini_key", value) }

    var openaiApiKey: String
        get() = prefs.getString("openai_key", "") ?: ""
        set(value) = prefs.edit { putString("openai_key", value) }

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit { putBoolean("onboarding_complete", value) }

    var userName: String
        get() = prefs.getString("user_name", "Vikram") ?: "Vikram"
        set(value) = prefs.edit { putString("user_name", value) }

    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", true)
        set(value) = prefs.edit { putBoolean("auto_start", value) }

    fun hasApiKey(): Boolean {
        return geminiApiKey.isNotBlank() || openaiApiKey.isNotBlank()
    }

    fun clearAll() {
        prefs.edit { clear() }
    }
}