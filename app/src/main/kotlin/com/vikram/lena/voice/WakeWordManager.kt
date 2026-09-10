package com.vikram.lena.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * FREE Wake Word Detection — Porcupine ki zarurat nahi!
 * 
 * METHOD 1: Continuous low-power speech recognition
 * METHOD 2: Audio energy detection + STT confirmation
 * 
 * Dono methods combine karke best result milta hai.
 */
class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private var isListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var energyDetectionJob: Job? = null

    // Wake words - variations handle karo
    private val wakeWords = listOf(
        "lena", "lina", "leena", "layna", "laina",
        "hey lena", "ok lena", "hi lena", "hello lena",
        "aye lena", "sun lena", "suno lena", "are lena",
        "arre lena", "oye lena"
    )

    // ========== METHOD 1: Continuous STT Based ==========

    fun startListening() {
        if (isListening) return
        isListening = true
        onStatusUpdate("Wake word listening started...")
        startContinuousRecognition()
    }

    private fun startContinuousRecognition() {
        if (!isListening) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStatusUpdate("🎧 Lena sun rahi hai...")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                matches?.forEach { text ->
                    checkForWakeWord(text.lowercase())
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                var wakeWordFound = false

                matches?.forEach { text ->
                    if (checkForWakeWord(text.lowercase())) {
                        wakeWordFound = true
                    }
                }

                // Restart listening if wake word not found
                if (!wakeWordFound && isListening) {
                    scope.launch {
                        delay(100) // Small delay to prevent rapid restart
                        startContinuousRecognition()
                    }
                }
            }

            override fun onError(error: Int) {
                // Restart on error (common in continuous mode)
                if (isListening) {
                    scope.launch {
                        delay(500)
                        startContinuousRecognition()
                    }
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                delay(1000)
                if (isListening) startContinuousRecognition()
            }
        }
    }

    private fun checkForWakeWord(text: String): Boolean {
        val found = wakeWords.any { wakeWord ->
            text.contains(wakeWord)
        }
        if (found) {
            onStatusUpdate("🎯 Wake word detected: \"$text\"")
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            onWakeWordDetected()
            return true
        }
        return false
    }

    // ========== METHOD 2: Energy Detection (Low Power) ==========

    fun startEnergyBasedDetection() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()

        energyDetectionJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            val silenceThreshold = 2000 // Adjust based on environment

            while (isActive && isListening) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readCount > 0) {
                    val energy = buffer.take(readCount).sumOf { abs(it.toInt()) } / readCount

                    if (energy > silenceThreshold) {
                        // Sound detected! STT se confirm karo
                        withContext(Dispatchers.Main) {
                            onStatusUpdate("🔊 Sound detected, checking...")
                            startContinuousRecognition()
                        }
                        delay(3000) // Wait for STT to process
                    }
                }
                delay(100) // Check every 100ms
            }
        }
    }

    // ========== HYBRID METHOD (BEST) ==========

    fun startHybridDetection() {
        isListening = true

        // Method 1: Continuous STT (primary)
        startContinuousRecognition()

        // Method 2 can be enabled for battery saving
        // startEnergyBasedDetection()
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        energyDetectionJob?.cancel()
        scope.cancel()
    }
}