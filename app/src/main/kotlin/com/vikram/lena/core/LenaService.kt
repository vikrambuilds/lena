package com.vikram.lena.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.vikram.lena.MainActivity
import com.vikram.lena.ai.AIManager
import com.vikram.lena.ai.TaskExecutor
import com.vikram.lena.data.ConversationManager
import com.vikram.lena.data.PreferencesManager
import com.vikram.lena.voice.VoiceManager
import kotlinx.coroutines.*

class LenaService : Service() {

    private lateinit var voiceManager: VoiceManager
    private lateinit var aiManager: AIManager
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var conversationManager: ConversationManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        const val CHANNEL_ID = "LenaServiceChannel"
        const val NOTIFICATION_ID = 1
        
        const val ACTION_START_LISTENING = "com.vikram.lena.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.vikram.lena.STOP_LISTENING"
        const val ACTION_STOP_SERVICE = "com.vikram.lena.STOP_SERVICE"
        
        var isRunning = false
            private set
        
        // Callbacks for UI to observe state
        var onStatusChanged: ((String) -> Unit)? = null
        var onVolumeChanged: ((Float) -> Unit)? = null
        var onNewMessage: (() -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        preferencesManager = PreferencesManager(this)
        conversationManager = ConversationManager(this)
        aiManager = AIManager(preferencesManager.geminiApiKey, preferencesManager.openaiApiKey)
        taskExecutor = TaskExecutor(this)
        voiceManager = VoiceManager(this)
        
        setupVoiceCallbacks()
        
        // WakeLock for background operation
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lena::WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes
        
        isRunning = true
    }

    private fun setupVoiceCallbacks() {
        voiceManager.onSpeechResult = { text ->
            handleUserMessage(text)
        }
        
        voiceManager.onSpeechError = { error ->
            updateStatus("❌ $error")
            voiceManager.speak(error)
        }
        
        voiceManager.onListeningStart = {
            updateStatus("🎤 Sun rahi hu...")
        }
        
        voiceManager.onListeningEnd = {
            updateStatus("💭 Process kar rahi hu...")
        }
        
        voiceManager.onVolumeChanged = { volume ->
            onVolumeChanged?.invoke(volume)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Ready to help! 🤖"))
        
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
            ACTION_STOP_SERVICE -> stopSelf()
        }
        
        return START_STICKY
    }

    fun startListening() {
        // Refresh API keys in case they changed
        aiManager.updateKeys(preferencesManager.geminiApiKey, preferencesManager.openaiApiKey)
        
        // Vibrate to give feedback
        vibrate()
        
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        updateStatus("Ready to help! 🤖")
    }

    private fun handleUserMessage(userMessage: String) {
        updateStatus("💭 \"$userMessage\"")
        
        // Save user message
        conversationManager.saveMessage("Vikram", userMessage)
        onNewMessage?.invoke()
        
        serviceScope.launch {
            try {
                // Step 1: Try offline task executor first
                val taskResult = withContext(Dispatchers.IO) {
                    taskExecutor.execute(userMessage)
                }
                
                val finalResponse: String
                val model: String
                
                if (taskResult.handled) {
                    // Offline task executed successfully
                    finalResponse = taskResult.response
                    model = "Offline"
                    updateStatus("✅ Task done!")
                } else {
                    // Need AI to respond
                    if (!aiManager.hasValidKey()) {
                        finalResponse = "Yaar, pehle Settings mein Gemini API key set karo! Free hai — aistudio.google.com se le le."
                        model = "NoKey"
                    } else {
                        updateStatus("🧠 AI se puchh rahi hu...")
                        val recentMessages = conversationManager.getRecentMessages(6)
                        val (aiResponse, aiModel) = aiManager.getResponse(userMessage, recentMessages)
                        finalResponse = aiResponse
                        model = aiModel
                    }
                }
                
                // Save Lena's response
                conversationManager.saveMessage("Lena", finalResponse, model)
                onNewMessage?.invoke()
                
                // Speak the response
                updateStatus("🗣️ Bol rahi hu...")
                voiceManager.speak(finalResponse) {
                    updateStatus("Ready to help! 🤖")
                }
                
            } catch (e: Exception) {
                val errorMsg = "Arre yaar, kuch gadbad ho gayi: ${e.message?.take(50)}"
                conversationManager.saveMessage("Lena", errorMsg, "Error")
                onNewMessage?.invoke()
                voiceManager.speak(errorMsg) {
                    updateStatus("Ready to help! 🤖")
                }
            }
        }
    }

    private fun updateStatus(status: String) {
        val notification = getSystemService(NotificationManager::class.java)
        notification.notify(NOTIFICATION_ID, createNotification(status))
        onStatusChanged?.invoke(status)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lena Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lena AI is ready to help"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val listenIntent = Intent(this, LenaService::class.java).apply {
            action = ACTION_START_LISTENING
        }
        val listenPendingIntent = PendingIntent.getService(
            this, 1, listenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lena AI 🤖")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "🎤 Listen", listenPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        voiceManager.destroy()
        if (wakeLock.isHeld) wakeLock.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}