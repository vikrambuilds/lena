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
import android.speech.tts.Voice
import java.util.*

/**
 * Advanced Voice Manager v2.2
 * - Sweet female Hindi voice
 * - Wake word detection ("Lena")
 * - Muted beep sounds
 * - Better recognition accuracy
 */
class VoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWakeWordMode = false // Continuous wake word listening
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeech: String? = null
    private var pendingCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    private var originalMusicVolume = 0
    private var originalSystemVolume = 0
    private var originalNotificationVolume = 0
    
    // Wake word variants (spelling variations for better detection)
    private val wakeWords = listOf(
        "lena", "leena", "lina", "layna", "laina",
        "hey lena", "ok lena", "hi lena", "hello lena",
        "arey lena", "arre lena", "oh lena", "sun lena"
    )

    // Callbacks
    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onListeningStart: (() -> Unit)? = null
    var onListeningEnd: (() -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null
    var onWakeWordDetected: (() -> Unit)? = null

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initTTS()
    }

    // ========== TTS INIT (Sweet Female Voice) ==========
    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupBestFemaleVoice()
                setupTtsListener()
                isTtsReady = true
                
                pendingSpeech?.let { text ->
                    speak(text, pendingCallback)
                    pendingSpeech = null
                }
            }
        }
    }

    private fun setupBestFemaleVoice() {
        // Try Hindi first
        val hindiIndia = Locale("hi", "IN")
        val hindiResult = tts?.setLanguage(hindiIndia)
        
        if (hindiResult == TextToSpeech.LANG_MISSING_DATA || 
            hindiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale("en", "IN"))
        }
        
        // Find best female voice
        val voices = tts?.voices ?: return
        
        val bestVoice = voices
            .filter { voice ->
                val name = voice.name.lowercase()
                val locale = voice.locale
                
                // Prefer Hindi voices
                (locale.language == "hi" || locale.language == "en") &&
                // Female voice indicators
                (name.contains("female") || 
                 name.contains("f#") ||
                 name.contains("wavenet-a") ||   // Google Wavenet Female
                 name.contains("wavenet-c") ||   // Google Wavenet Female
                 name.contains("wavenet-e") ||   // Google Wavenet Female
                 name.contains("neural2-a") ||   // Google Neural2 Female
                 name.contains("neural2-c") ||   // Google Neural2 Female
                 name.contains("standard-a") ||  // Google Standard Female
                 name.contains("hi-in-x-hie") || // Hindi Female
                 name.contains("hi-in-x-hia") || // Hindi Female
                 name.contains("en-in-x-ene")) && // Indian English Female
                !voice.isNetworkConnectionRequired
            }
            .maxByOrNull { voice ->
                var score = 0
                val name = voice.name.lowercase()
                if (voice.locale.language == "hi") score += 100
                if (name.contains("wavenet")) score += 50
                if (name.contains("neural")) score += 40
                if (voice.quality >= 400) score += 30
                if (name.contains("female") || name.contains("-a") || name.contains("-c")) score += 20
                score
            }
        
        if (bestVoice != null) {
            tts?.voice = bestVoice
        } else {
            // Fallback: try any Hindi/Indian voice
            val fallbackVoice = voices.firstOrNull { 
                it.locale.language == "hi" || 
                (it.locale.language == "en" && it.locale.country == "IN")
            }
            fallbackVoice?.let { tts?.voice = it }
        }
        
        // Sweet feminine voice tuning
        tts?.setPitch(1.15f)      // Higher pitch = feminine, sweet
        tts?.setSpeechRate(0.9f)  // Slightly slow = clear, warm
    }
    
    private fun setupTtsListener() {
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

    // ========== MUTE BEEP SOUNDS ==========
    private fun muteBeepSounds() {
        audioManager?.let { am ->
            try {
                // Store original volumes
                originalMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                originalSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
                originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                
                // Mute system beeps (but keep music/TTS)
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            } catch (e: Exception) {
                // Some devices don't allow this - ignore
            }
        }
    }
    
    private fun restoreBeepSounds() {
        audioManager?.let { am ->
            try {
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // ========== WAKE WORD LISTENING (Continuous) ==========
    fun startWakeWordListening() {
        isWakeWordMode = true
        startWakeWordCycle()
    }
    
    fun stopWakeWordListening() {
        isWakeWordMode = false
        stopListening()
    }
    
    private fun startWakeWordCycle() {
        if (!isWakeWordMode) return
        
        mainHandler.post {
            try {
                muteBeepSounds()
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        matches?.forEach { text ->
                            if (containsWakeWord(text.lowercase())) {
                                onWakeWordActivated()
                                return
                            }
                        }
                    }
                    
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        var found = false
                        matches?.forEach { text ->
                            if (containsWakeWord(text.lowercase())) {
                                onWakeWordActivated()
                                found = true
                                return@forEach
                            }
                        }
                        
                        // Continue listening if not found
                        if (!found && isWakeWordMode) {
                            mainHandler.postDelayed({ startWakeWordCycle() }, 100)
                        }
                    }
                    
                    override fun onError(error: Int) {
                        // Silently restart on error (common in continuous mode)
                        if (isWakeWordMode) {
                            mainHandler.postDelayed({ startWakeWordCycle() }, 500)
                        }
                    }
                })
                
                speechRecognizer?.startListening(intent)
                
            } catch (e: Exception) {
                if (isWakeWordMode) {
                    mainHandler.postDelayed({ startWakeWordCycle() }, 1000)
                }
            }
        }
    }
    
    private fun containsWakeWord(text: String): Boolean {
        return wakeWords.any { text.contains(it) }
    }
    
    private fun onWakeWordActivated() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        onWakeWordDetected?.invoke()
    }

    // ========== NORMAL COMMAND LISTENING ==========
    fun startListening() {
        if (isListening) return
        
        mainHandler.post {
            try {
                if (isSpeaking()) tts?.stop()
                
                muteBeepSounds()
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    // Multi-language for better accuracy
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStart?.invoke()
                    }
                    
                    override fun onBeginningOfSpeech() {}
                    
                    override fun onRmsChanged(rmsdB: Float) {
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
                        restoreBeepSounds()
                        onListeningEnd?.invoke()
                        
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val confidences = results?.getFloatArray(
                            SpeechRecognizer.CONFIDENCE_SCORES
                        )
                        
                        if (!matches.isNullOrEmpty()) {
                            // Pick best result based on confidence
                            val bestResult = if (confidences != null && confidences.isNotEmpty()) {
                                val bestIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
                                matches[bestIndex.coerceAtMost(matches.size - 1)]
                            } else {
                                matches[0]
                            }
                            onSpeechResult?.invoke(bestResult)
                        } else {
                            onSpeechError?.invoke("Kuch samjh nahi aaya yaar!")
                        }
                    }
                    
                    override fun onError(error: Int) {
                        isListening = false
                        restoreBeepSounds()
                        onListeningEnd?.invoke()
                        
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Kuch clear nahi bola tune!"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Time out ho gaya, dobara try kar!"
                            SpeechRecognizer.ERROR_NETWORK -> "Internet check kar!"
                            SpeechRecognizer.ERROR_AUDIO -> "Mic mein problem hai!"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission de yaar!"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> ""
                            SpeechRecognizer.ERROR_CLIENT -> ""
                            else -> ""
                        }
                        
                        if (errorMsg.isNotEmpty()) {
                            onSpeechError?.invoke(errorMsg)
                        }
                    }
                })
                
                speechRecognizer?.startListening(intent)
                
            } catch (e: Exception) {
                isListening = false
                restoreBeepSounds()
                onSpeechError?.invoke("Voice recognition start nahi ho paya!")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            restoreBeepSounds()
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    // ========== CLEANUP ==========
    fun destroy() {
        isWakeWordMode = false
        stopListening()
        restoreBeepSounds()
        
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // ========== HELPER ==========
    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[\\p{So}\\p{Sk}]"), "")
            .replace(Regex("[*#`_~]"), "")
            .replace(Regex("\\n+"), ". ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}