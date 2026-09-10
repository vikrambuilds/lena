package com.vikram.lena.voice

import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor

/**
 * Noise Cancellation Manager
 * 
 * Uses Android's built-in audio effects:
 * 1. NoiseSuppressor - Background noise reduce karta hai
 * 2. AcousticEchoCanceler - Echo cancel karta hai
 * 3. AutomaticGainControl - Volume normalize karta hai
 */
class NoiseCancellation {

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    /** Attach noise cancellation to AudioRecord session */
    fun attachToAudioSession(audioSessionId: Int): Boolean {
        var success = true

        // 1. Noise Suppression
        if (NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                noiseSuppressor?.enabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }
        }

        // 2. Echo Cancellation
        if (AcousticEchoCanceler.isAvailable()) {
            try {
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)
                echoCanceler?.enabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }
        }

        // 3. Automatic Gain Control
        if (AutomaticGainControl.isAvailable()) {
            try {
                gainControl = AutomaticGainControl.create(audioSessionId)
                gainControl?.enabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }
        }

        return success
    }

    /** Process audio buffer for additional noise reduction */
    fun processBuffer(buffer: ByteArray): ByteArray {
        // Simple noise gate implementation
        val threshold: Byte = 10  // Adjust based on environment

        for (i in buffer.indices) {
            if (kotlin.math.abs(buffer[i].toInt()) < threshold) {
                buffer[i] = 0
            }
        }

        return buffer
    }

    /** Process short array buffer */
    fun processShortBuffer(buffer: ShortArray, size: Int): ShortArray {
        val threshold: Short = 500  // Noise floor threshold

        for (i in 0 until size) {
            if (kotlin.math.abs(buffer[i].toInt()) < threshold) {
                buffer[i] = 0
            }
        }

        return buffer
    }

    /** Calculate audio energy (for voice activity detection) */
    fun calculateEnergy(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        return kotlin.math.sqrt(sum / size)
    }

    /** Check if audio contains voice (Voice Activity Detection) */
    fun isVoiceDetected(buffer: ShortArray, size: Int): Boolean {
        val energy = calculateEnergy(buffer, size)
        val voiceThreshold = 2000.0  // Adjust based on testing
        return energy > voiceThreshold
    }

    /** Get status of noise cancellation features */
    fun getStatus(): Map<String, Boolean> {
        return mapOf(
            "NoiseSuppressor" to (noiseSuppressor?.enabled == true),
            "EchoCanceler" to (echoCanceler?.enabled == true),
            "GainControl" to (gainControl?.enabled == true),
            "NS_Available" to NoiseSuppressor.isAvailable(),
            "AEC_Available" to AcousticEchoCanceler.isAvailable(),
            "AGC_Available" to AutomaticGainControl.isAvailable()
        )
    }

    /** Release all resources */
    fun release() {
        try {
            noiseSuppressor?.enabled = false
            noiseSuppressor?.release()
            noiseSuppressor = null

            echoCanceler?.enabled = false
            echoCanceler?.release()
            echoCanceler = null

            gainControl?.enabled = false
            gainControl?.release()
            gainControl = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}