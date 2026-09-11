package com.vikram.lena.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

/**
 * Unified Voice Manager - Simple, Reliable, Working
 * Handles both STT (Speech-to-Text) and TTS (Text-to-Speech)
 */
class VoiceManager(private val context: Context) {

    // ========== STT ==========
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // ========== TTS ==========
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeech: String? = null
    private var pendingCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // Callbacks
    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onListeningStart: (() -> Unit)? = null
    var onListeningEnd: (() -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null

    init {
        initTTS()
    }

    // ========== TTS INIT ==========
    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try Hindi first, fallback to English
                val hindiResult = tts?.setLanguage(Locale("hi", "IN"))
                if (hindiResult == TextToSpeech.LANG_MISSING_DATA ||
                    hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                
                tts?.setPitch(1.1f)
                tts?.setSpeechRate(0.95f)
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            pendingCallback?.invoke()
                            pendingCallback = null
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            pendingCallback?.invoke()
                            pendingCallback = null
                        }
                    }
                })
                
                isTtsReady = true
                
                // Play any pending speech
                pendingSpeech?.let { text ->
                    speak(text, pendingCallback)
                    pendingSpeech = null
                }
            }
        }
    }

    // ========== SPEAK ==========
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady) {
            pendingSpeech = text
            pendingCallback = onComplete
            return
        }
        
        pendingCallback = onComplete
        val cleanText = cleanTextForSpeech(text)
        
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "lena_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    // ========== START LISTENING ==========
    fun startListening() {
        if (isListening) return
        
        mainHandler.post {
            try {
                // Stop any ongoing speech first
                if (isSpeaking()) tts?.stop()
                
                // Cleanup old recognizer
                speechRecognizer?.destroy()
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStart?.invoke()
                    }
                    
                    override fun onBeginningOfSpeech() {}
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize to 0-1 range for animation
                        val normalized = ((rmsdB + 10f) / 20f).coerceIn(0f, 1f)
                        onVolumeChanged?.invoke(normalized)
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    
                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningEnd?.invoke()
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningEnd?.invoke()
                        
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        if (!matches.isNullOrEmpty()) {
                            onSpeechResult?.invoke(matches[0])
                        } else {
                            onSpeechError?.invoke("Kuch samjh nahi aaya yaar!")
                        }
                    }
                    
                    override fun onError(error: Int) {
                        isListening = false
                        onListeningEnd?.invoke()
                        
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Kuch clear nahi bola tune!"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Time out ho gaya, dobara try kar!"
                            SpeechRecognizer.ERROR_NETWORK -> "Internet check kar!"
                            SpeechRecognizer.ERROR_AUDIO -> "Mic mein problem hai!"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission de yaar!"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ruk ja thoda, busy hu!"
                            SpeechRecognizer.ERROR_CLIENT -> "" // Ignore, common error
                            else -> "Error $error aayi!"
                        }
                        
                        if (errorMsg.isNotEmpty()) {
                            onSpeechError?.invoke(errorMsg)
                        }
                    }
                })
                
                speechRecognizer?.startListening(intent)
                
            } catch (e: Exception) {
                isListening = false
                onSpeechError?.invoke("Voice recognition start nahi ho paya: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    // ========== CLEANUP ==========
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // ========== HELPER ==========
    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[\\p{So}\\p{Sk}]"), "") // Remove emojis
            .replace(Regex("[*#`_~]"), "")           // Remove markdown
            .replace(Regex("\\n+"), ". ")            // Newlines to pauses
            .replace(Regex("\\s+"), " ")             // Multiple spaces
            .trim()
    }
}