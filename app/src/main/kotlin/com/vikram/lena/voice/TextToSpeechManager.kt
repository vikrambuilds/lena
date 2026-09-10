package com.vikram.lena.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var onSpeakComplete: (() -> Unit)? = null

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupLanguage()
                setupVoice()
                setupListener()
                isReady = true
            }
        }
    }

    private fun setupLanguage() {
        // Try Hindi first
        val hindiLocale = Locale("hi", "IN")
        val result = tts?.setLanguage(hindiLocale)

        when (result) {
            TextToSpeech.LANG_MISSING_DATA, 
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                // Fallback: Try Hindi without country
                val hindiResult = tts?.setLanguage(Locale("hi"))
                if (hindiResult == TextToSpeech.LANG_MISSING_DATA ||
                    hindiResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Final fallback: English India
                    tts?.setLanguage(Locale("en", "IN"))
                }
            }
        }
    }

    private fun setupVoice() {
        // Try to find a female Hindi voice
        tts?.voices?.let { voices ->
            val hindiVoice = voices.firstOrNull { voice ->
                voice.locale.language == "hi" &&
                        !voice.isNetworkConnectionRequired &&
                        voice.quality >= 300
            }

            val hindiNetworkVoice = voices.firstOrNull { voice ->
                voice.locale.language == "hi" &&
                        voice.quality >= 300
            }

            (hindiVoice ?: hindiNetworkVoice)?.let { selectedVoice ->
                tts?.voice = selectedVoice
            }
        }

        // Voice settings - friendly, warm
        tts?.setPitch(1.05f)        // Slightly higher pitch (feminine)
        tts?.setSpeechRate(0.92f)   // Slightly slower (natural)
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                requestAudioFocus()
            }

            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
                onSpeakComplete?.invoke()
                onSpeakComplete = null
            }

            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
                onSpeakComplete?.invoke()
                onSpeakComplete = null
            }

            @Deprecated("Deprecated in API")
            override fun onError(utteranceId: String?, errorCode: Int) {
                abandonAudioFocus()
                onSpeakComplete?.invoke()
                onSpeakComplete = null
            }
        })
    }

    /** Speak text with callback when done */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isReady) {
            onComplete?.invoke()
            return
        }

        onSpeakComplete = onComplete

        // Clean text for better TTS output
        val cleanText = cleanTextForSpeech(text)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "lena_speech_${System.currentTimeMillis()}")
    }

    /** Speak and add to queue (don't interrupt) */
    fun speakAdd(text: String) {
        if (!isReady) return

        val cleanText = cleanTextForSpeech(text)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_ADD, params, "lena_queue_${System.currentTimeMillis()}")
    }

    /** Clean text for better TTS pronunciation */
    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace("💪", "")
            .replace("😊", "")
            .replace("😅", "")
            .replace("🔥", "")
            .replace("❤️", "")
            .replace("👍", "")
            .replace("📞", "")
            .replace("📱", "")
            .replace("✅", "")
            .replace("❌", "")
            .replace("⚠️", "")
            .replace("🎵", "")
            .replace("⏰", "")
            .replace("🔋", "")
            .replace("🪫", "")
            .replace("⚡", "")
            .replace("📶", "")
            .replace("🔦", "")
            .replace("🔊", "")
            .replace("🔉", "")
            .replace("🔇", "")
            .replace("🔵", "")
            .replace("💡", "")
            .replace("📅", "")
            .replace("📊", "")
            .replace("📋", "")
            .replace("💬", "")
            .replace("📥", "")
            .replace("📤", "")
            .replace("🗑️", "")
            .replace("🚫", "")
            .replace("🤖", "")
            .replace("🎧", "")
            .replace("🎤", "")
            .replace("💭", "")
            .replace("🗣️", "")
            .replace("📺", "")
            .replace("📚", "")
            .replace("🍕", "")
            .replace("😴", "")
            .replace("☀️", "")
            .replace("⏸️", "")
            .replace("⏭️", "")
            .replace("🙋‍♂️", "")
            .replace(Regex("[\\p{So}\\p{Sk}]"), "") // Remove remaining emojis
            .replace("\\*+".toRegex(), "")           // Remove markdown bold
            .replace("#", "")                         // Remove headers
            .replace("`", "")                         // Remove code marks
            .replace("\n\n", ". ")                    // Double newlines to pause
            .replace("\n", " ")                       // Single newline to space
            .trim()
    }

    /** Request audio focus so other audio stops */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                .build()
            focusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    fun stop() {
        tts?.stop()
        abandonAudioFocus()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    /** Change speech rate at runtime */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    /** Change pitch at runtime */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /** Get available languages */
    fun getAvailableLanguages(): List<Locale> {
        return tts?.availableLanguages?.toList() ?: emptyList()
    }
}