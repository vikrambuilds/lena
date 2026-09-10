package com.vikram.lena.automation

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

class MediaController(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Play/Resume music */
    fun play(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        return "Music play kar di! 🎵"
    }

    /** Pause music */
    fun pause(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        return "Music pause kar di! ⏸️"
    }

    /** Play/Pause toggle */
    fun playPause(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        return if (audioManager.isMusicActive)
            "Music pause kar di! ⏸️"
        else
            "Music play kar di! 🎵"
    }

    /** Next track */
    fun next(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        return "Agla gaana laga rahi hu! ⏭️"
    }

    /** Previous track */
    fun previous(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return "Pichla gaana laga rahi hu! ⏮️"
    }

    /** Stop music */
    fun stop(): String {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_STOP)
        return "Music band kar di! ⏹️"
    }

    /** Is music currently playing? */
    fun isMusicPlaying(): Boolean {
        return audioManager.isMusicActive
    }

    /** Get music status */
    fun getMusicStatus(): String {
        return if (audioManager.isMusicActive)
            "Haan yaar, abhi music chal raha hai! 🎵"
        else
            "Nahi yaar, abhi koi music nahi chal raha."
    }

    /** Open music player */
    fun openMusicPlayer(): String {
        return try {
            // Try popular music apps
            val musicApps = listOf(
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.gaana",
                "com.jio.media.jiobeats",
                "com.amazon.mp3"
            )

            var launched = false
            for (packageName in musicApps) {
                val intent = context.packageManager
                    .getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    launched = true
                    break
                }
            }

            if (!launched) {
                // Open default music intent
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            "Music app khol rahi hu! 🎶"
        } catch (e: Exception) {
            "Koi music app nahi mila yaar!"
        }
    }

    // ========== HELPER ==========

    private fun sendMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_DOWN, keyCode, 0
        )
        val upEvent = KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_UP, keyCode, 0
        )

        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}