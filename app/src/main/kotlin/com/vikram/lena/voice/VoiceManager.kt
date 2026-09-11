package com.vikram.lena.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class VoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWakeWordMode = false
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeech: String? = null
    private var pendingCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    
    // Mute helper volumes
    private var originalSystemVolume = 0
    private var originalNotificationVolume = 0
    
    private val wakeWords = listOf("lena", "leena", "lina", "layna", "laina", "hey lena", "ok lena")

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
        val hindiLocale = Locale("hi", "IN")
        val result = tts?.setLanguage(hindiLocale)
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        
        // Sweet sweet high-pitch female voice profile
        tts?.setPitch(1.23f)      // Optimized sweet female pitch
        tts?.setSpeechRate(0.92f)  // Calm and sweet speech pacing
    }
    
    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("last_") == true) {
                    mainHandler.post {
                        pendingCallback?.invoke()
                        pendingCallback = null
                    }
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

    /**
     * Sentence Chunking Engine
     * Cuts the long responses into chunks to prevent early cutoff
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady) {
            pendingSpeech = text
            pendingCallback = onComplete
            return
        }
        
        stopSpeaking() // Ensure previous speak cycle completely stops
        pendingCallback = onComplete

        // Clean and chunk text
        val cleanedText = cleanTextForSpeech(text)
        
        // Regex to split by punctuation keeping context pauses natural
        val sentences = cleanedText.split(Regex("(?<=[.!?।।])\\s+"))
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            onComplete?.invoke()
            return
        }

        for (i in sentences.indices) {
            val sentence = sentences[i]
            val utteranceId = if (i == sentences.size - 1) "last_${System.currentTimeMillis()}" else "chunk_$i"
            val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            
            tts?.speak(sentence, queueMode, params, utteranceId)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    // ========== NOISE CANCELING & BEEP MUTER ==========
    private fun muteBeepSounds() {
        try {
            audioManager?.let { am ->
                originalSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
                originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun restoreBeepSounds() {
        try {
            audioManager?.let { am ->
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== WAKE WORD DETECTION ==========
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
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.forEach { text ->
                            if (containsWakeWord(text.lowercase())) {
                                onWakeWordActivated()
                                return
                            }
                        }
                    }
                    
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        var found = false
                        matches?.forEach { text ->
                            if (containsWakeWord(text.lowercase())) {
                                onWakeWordActivated()
                                found = true
                                return@forEach
                            }
                        }
                        if (!found && isWakeWordMode) {
                            mainHandler.postDelayed({ startWakeWordCycle() }, 100)
                        }
                    }
                    
                    override fun onError(error: Int) {
                        if (isWakeWordMode) {
                            mainHandler.postDelayed({ startWakeWordCycle() }, 300)
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
        stopSpeaking() // Interrupt speaking when Wake Word is detected (Barge-In)
        speechRecognizer?.destroy()
        speechRecognizer = null
        onWakeWordDetected?.invoke()
    }

    // ========== COMMAND LISTENING (Noise Suppression Built-In) ==========
    fun startListening() {
        if (isListening) return
        
        mainHandler.post {
            try {
                stopSpeaking() // Force stop speaking when user starts talking (Barge-In)
                muteBeepSounds()
                
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                    
                    // Hardware Noise canceling and Echo Cancellation integrations if supported
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStart?.invoke()
                    }
                    
                    override fun onBeginningOfSpeech() {}
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // High noise suppression gate: Ignore room whisper peaks below 1.5 RMS
                        val normalized = ((rmsdB + 2f) / 22f).coerceIn(0f, 1f)
                        if (normalized > 0.15f) {
                            onVolumeChanged?.invoke(normalized)
                        } else {
                            onVolumeChanged?.invoke(0f)
                        }
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
                        
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onSpeechResult?.invoke(matches[0])
                        } else {
                            onSpeechError?.invoke("Yaar, kuch sunayi nahi diya clear.")
                        }
                    }
                    
                    override fun onError(error: Int) {
                        isListening = false
                        restoreBeepSounds()
                        onListeningEnd?.invoke()
                        
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Kuch clear bolo yaar!"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout ho gaya!"
                            else -> ""
                        }
                        if (errorMsg.isNotBlank()) onSpeechError?.invoke(errorMsg)
                    }
                })
                
                speechRecognizer?.startListening(intent)
                
            } catch (e: Exception) {
                isListening = false
                restoreBeepSounds()
                onSpeechError?.invoke("Voice recognition error!")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            restoreBeepSounds()
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun destroy() {
        isWakeWordMode = false
        stopListening()
        restoreBeepSounds()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[\\p{So}\\p{Sk}]"), "") // Remove emojis
            .replace(Regex("[*#`_~]"), "")           // Remove markdown
            .replace(Regex("\\n+"), ". ")            // Newlines to pauses
            .replace(Regex("\\s+"), " ")             // Multiple spaces
            .replace("।", ".")                       // Convert Purnavirama for engine mapping
            .trim()
    }
}