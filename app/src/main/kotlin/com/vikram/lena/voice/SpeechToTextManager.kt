package com.vikram.lena.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechToTextManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onListeningStarted: (() -> Unit)? = null,
    private val onListeningStopped: (() -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Noise cancellation helper
    private val noiseCancellation = NoiseCancellation()

    fun startListening() {
        if (isListening) return

        mainHandler.post {
            try {
                isListening = true
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    // Multiple languages support (Hindi + English + Hinglish)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN"
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
                        false
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // Longer silence detection for natural speech
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        2000L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        1500L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        500L
                    )

                    // Enable noise cancellation if available
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(
                        "android.speech.extra.ENABLE_FORMATTING",
                        "android.speech.extra.ENABLE_FORMATTING"
                    )
                }

                speechRecognizer?.setRecognitionListener(createListener())
                speechRecognizer?.startListening(intent)

            } catch (e: Exception) {
                isListening = false
                onError("Speech recognition start nahi ho paya: ${e.message}")
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStarted?.invoke()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                // Can be used for voice wave animation
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Apply noise cancellation to buffer
                buffer?.let {
                    noiseCancellation.processBuffer(it)
                }
            }

            override fun onEndOfSpeech() {
                isListening = false
                onListeningStopped?.invoke()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                if (!matches.isNullOrEmpty()) {
                    onPartialResult?.invoke(matches[0])
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                if (!matches.isNullOrEmpty()) {
                    // Clean up the recognized text
                    val cleanText = cleanRecognizedText(matches[0])
                    if (cleanText.isNotBlank()) {
                        onResult(cleanText)
                    } else {
                        onError("Kuch samjh nahi aaya yaar, dobara bol!")
                    }
                } else {
                    onError("Kuch suna nahi yaar, dobara try kar!")
                }
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH ->
                        "Samjh nahi aaya yaar, zara clear bolo!"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "Kuch bola nahi tune, dobara try kar!"
                    SpeechRecognizer.ERROR_NETWORK ->
                        "Internet check kar yaar, network issue hai!"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network timeout ho gaya, dobara try kar!"
                    SpeechRecognizer.ERROR_AUDIO ->
                        "Mic mein problem hai yaar!"
                    SpeechRecognizer.ERROR_SERVER ->
                        "Google server mein issue hai, thodi der baad try kar!"
                    SpeechRecognizer.ERROR_CLIENT ->
                        "App mein kuch gadbad hai!"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Mic permission de yaar!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        "Ruk ja yaar, abhi busy hai!"
                    else ->
                        "Kuch gadbad ho gayi (Error: $error)"
                }
                onError(errorMsg)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /** Clean up recognized text */
    private fun cleanRecognizedText(text: String): String {
        return text
            .trim()
            .replace("\\s+".toRegex(), " ")      // Multiple spaces to single
            .replaceFirstChar { it.uppercase() }   // Capitalize first letter
    }

    fun stopListening() {
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun destroy() {
        stopListening()
    }
}