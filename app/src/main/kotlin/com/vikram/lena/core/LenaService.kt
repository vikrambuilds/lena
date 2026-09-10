package com.vikram.lena.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.vikram.lena.MainActivity
import com.vikram.lena.ai.AIManager
import com.vikram.lena.ai.TaskClassifier
import com.vikram.lena.ai.TaskType
import com.vikram.lena.automation.*
import com.vikram.lena.data.ConversationManager
import com.vikram.lena.voice.SpeechToTextManager
import com.vikram.lena.voice.TextToSpeechManager
import com.vikram.lena.voice.WakeWordManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LenaService : Service() {

    // Managers
    private lateinit var wakeWordManager: WakeWordManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var aiManager: AIManager
    private lateinit var conversationManager: ConversationManager
    private lateinit var taskClassifier: TaskClassifier
    
    // Automation Managers
    private lateinit var phoneCallManager: PhoneCallManager
    private lateinit var appLauncher: AppLauncher
    private lateinit var systemController: SystemController
    private lateinit var smsManager: SMSManager
    private lateinit var whatsAppManager: WhatsAppManager
    
    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var sttManager: SpeechToTextManager? = null
    private var lastGreetingHour = -1

    companion object {
        const val CHANNEL_ID = "LenaServiceChannel"
        const val NOTIFICATION_ID = 1

        // API Keys (Settings se bhi set kar sakte ho)
        var GEMINI_API_KEY = ""
        var OPENAI_API_KEY = ""
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Load API keys from preferences
        val prefs = getSharedPreferences("lena_prefs", Context.MODE_PRIVATE)
        GEMINI_API_KEY = prefs.getString("gemini_key", "") ?: ""
        OPENAI_API_KEY = prefs.getString("openai_key", "") ?: ""

        // Initialize all managers
        conversationManager = ConversationManager(this)
        aiManager = AIManager(GEMINI_API_KEY, OPENAI_API_KEY)
        ttsManager = TextToSpeechManager(this)
        taskClassifier = TaskClassifier()

        // Automation
        phoneCallManager = PhoneCallManager(this)
        appLauncher = AppLauncher(this)
        systemController = SystemController(this)
        smsManager = SMSManager(this)
        whatsAppManager = WhatsAppManager(this)

        // WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lena::WakeLock"
        )
        wakeLock.acquire()

        // Wake Word Manager (FREE!)
        wakeWordManager = WakeWordManager(
            context = this,
            onWakeWordDetected = { onLenaWakeUp() },
            onStatusUpdate = { status -> updateNotification(status) }
        )

        // Proactive greeting
        sendProactiveGreeting()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("🎧 Lena sun rahi hai..."))
        
        // Start wake word detection
        wakeWordManager.startHybridDetection()
        
        return START_STICKY
    }

    /** Jab "Lena" detect ho */
    private fun onLenaWakeUp() {
        updateNotification("🎤 Bolo Vikram, Lena sun rahi hai...")

        // Vibrate to indicate listening
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(
                Context.VIBRATOR_MANAGER_SERVICE
            ) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

        // Start listening
        sttManager = SpeechToTextManager(
            context = this,
            onResult = { userMessage -> processUserMessage(userMessage) },
            onError = { error ->
                ttsManager.speak("Samjh nahi aaya yaar, dobara bol!") {
                    restartWakeWord()
                }
            }
        )
        sttManager?.startListening()
    }

    /** Main processing pipeline */
    private fun processUserMessage(userMessage: String) {
        updateNotification("💭 Lena soch rahi hai...")

        val startTime = System.currentTimeMillis()

        // 1. Classify the task
        val parsedTask = taskClassifier.classify(userMessage)

        serviceScope.launch {
            try {
                var taskResult: String? = null
                var responseText: String
                var modelUsed = ""

                // 2. Execute task if needed
                if (parsedTask.type != TaskType.CONVERSATION) {
                    taskResult = executeTask(parsedTask)
                }

                // 3. Get AI response (with task context)
                val recentMessages = conversationManager.getRecentMessages(10)
                
                if (parsedTask.type == TaskType.CONVERSATION || taskResult != null) {
                    val (aiResponse, model) = aiManager.getResponse(
                        userMessage, recentMessages, taskResult
                    )
                    responseText = aiResponse
                    modelUsed = model
                } else {
                    responseText = taskResult ?: "Ho gaya yaar! ✅"
                    modelUsed = "Local"
                }

                val responseTime = System.currentTimeMillis() - startTime

                // 4. Save to CSV
                conversationManager.saveMessage(
                    sender = "Vikram",
                    message = userMessage,
                    taskType = parsedTask.type.name
                )
                conversationManager.saveMessage(
                    sender = "Lena",
                    message = responseText,
                    aiModel = modelUsed,
                    taskType = parsedTask.type.name,
                    responseTime = responseTime
                )

                // 5. Speak response
                withContext(Dispatchers.Main) {
                    updateNotification("🗣️ Lena bol rahi hai...")
                    ttsManager.speak(responseText) {
                        restartWakeWord()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = "Arre yaar, kuch gadbad ho gayi. " +
                        "Dobara try kar! 😅"
                    conversationManager.saveMessage("Lena", errorMsg, "Error")
                    ttsManager.speak(errorMsg) {
                        restartWakeWord()
                    }
                }
            }
        }
    }

    /** Execute phone tasks */
    private fun executeTask(task: com.vikram.lena.ai.ParsedTask): String {
        return when (task.type) {
            TaskType.MAKE_CALL -> {
                val contact = task.params["contact"] ?: ""
                phoneCallManager.makeCall(contact)
            }
            TaskType.ANSWER_CALL -> phoneCallManager.answerCall()
            TaskType.REJECT_CALL -> phoneCallManager.rejectCall()
            
            TaskType.OPEN_APP -> {
                val appName = task.params["appName"] ?: ""
                appLauncher.openApp(appName)
            }
            
            TaskType.SEND_SMS -> {
                val contact = task.params["contact"] ?: ""
                val message = task.params["message"] ?: ""
                smsManager.sendSMS(contact, message)
            }
            TaskType.READ_SMS -> smsManager.readRecentSMS()
            
            TaskType.SEND_WHATSAPP -> {
                val contact = task.params["contact"] ?: ""
                val message = task.params["message"] ?: ""
                whatsAppManager.sendWhatsAppMessage(contact, message)
            }
            
            TaskType.WIFI_TOGGLE -> {
                val turnOn = task.params["action"] == "on"
                systemController.toggleWifi(turnOn)
            }
            TaskType.BLUETOOTH_TOGGLE -> {
                val turnOn = task.params["action"] == "on"
                systemController.toggleBluetooth(turnOn)
            }
            TaskType.FLASHLIGHT_TOGGLE -> {
                val turnOn = task.params["action"] == "on"
                systemController.toggleFlashlight(turnOn)
            }
            
            TaskType.VOLUME_CONTROL -> {
                val action = task.params["action"] ?: "up"
                systemController.controlVolume(action)
            }
            TaskType.BRIGHTNESS_CONTROL -> {
                val action = task.params["action"] ?: "up"
                systemController.controlBrightness(action)
            }
            
            TaskType.PLAY_MUSIC -> systemController.controlMedia("play")
            TaskType.PAUSE_MUSIC -> systemController.controlMedia("pause")
            TaskType.NEXT_TRACK -> systemController.controlMedia("next")
            
            TaskType.SET_ALARM -> {
                val hour = task.params["hour"]?.toIntOrNull() ?: 7
                val minute = task.params["minute"]?.toIntOrNull() ?: 0
                systemController.setAlarm(hour, minute)
            }
            
            TaskType.BATTERY_STATUS -> systemController.getBatteryStatus()
            TaskType.TIME_DATE -> systemController.getTimeDate()
            
            TaskType.SEARCH_CONTACT -> {
                val name = task.params["name"] ?: ""
                phoneCallManager.searchContact(name)
            }
            
            else -> "Ye task abhi nahi kar sakti yaar!"
        }
    }

    /** Proactive greeting based on time */
    private fun sendProactiveGreeting() {
        serviceScope.launch {
            while (true) {
                delay(60000) // Check every minute
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                
                if (hour != lastGreetingHour) {
                    val greeting = when (hour) {
                        7 -> "Good morning Vikram! ☀️ Uth gaya? Aaj ka din mast hone wala hai!"
                        13 -> "Vikram yaar, lunch kar liya? Break le le thoda! 🍕"
                        18 -> "Good evening yaar! Aaj ki padhai kaisi rahi? 📚"
                        22 -> "Vikram, raat ho gayi. Jaldi so ja yaar, health important hai! 😴"
                        else -> null
                    }
                    
                    if (greeting != null) {
                        lastGreetingHour = hour
                        conversationManager.saveMessage(
                            "Lena", greeting, "Proactive", "GREETING"
                        )
                        withContext(Dispatchers.Main) {
                            ttsManager.speak(greeting)
                        }
                    }
                }
            }
        }
    }

    private fun restartWakeWord() {
        updateNotification("🎧 Lena sun rahi hai...")
        wakeWordManager.startHybridDetection()
    }

    // ========== NOTIFICATION ==========

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lena AI Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lena is always ready to help!"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lena AI 🤖")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(status))
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordManager.stopListening()
        ttsManager.shutdown()
        sttManager?.stopListening()
        if (wakeLock.isHeld) wakeLock.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}