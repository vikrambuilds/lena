package com.vikram.lena.ai

import android.content.Context
import com.vikram.lena.automation.*

/**
 * Offline Task Executor
 * Bina AI ke bhi ye tasks execute karega
 * Simple keyword matching se
 */
class TaskExecutor(private val context: Context) {

    private val phoneCallManager = PhoneCallManager(context)
    private val appLauncher = AppLauncher(context)
    private val systemController = SystemController(context)
    private val smsManager = SMSManager(context)
    private val whatsAppManager = WhatsAppManager(context)
    private val contactManager = ContactManager(context)
    private val mediaController = MediaController(context)
    private val alarmScheduler = AlarmScheduler(context)
    private val deviceInfoManager = DeviceInfoManager(context)

    data class TaskResult(
        val handled: Boolean,       // Was this task handled offline?
        val response: String,       // Response to speak
        val requiresAI: Boolean = false // Should we also call AI?
    )

    fun execute(userMessage: String): TaskResult {
        val msg = userMessage.lowercase().trim()

        return when {
            // ========== GREETINGS ==========
            isGreeting(msg) -> TaskResult(true, getGreetingResponse())
            
            // ========== TIME & DATE ==========
            containsAny(msg, listOf("time", "samay", "kitne baje", "kya time", "waqt")) -> 
                TaskResult(true, deviceInfoManager.getTimeAndDate())
            
            containsAny(msg, listOf("date", "tarikh", "din kya", "aaj kya din")) -> 
                TaskResult(true, deviceInfoManager.getTimeAndDate())
            
            // ========== BATTERY ==========
            containsAny(msg, listOf("battery", "charge", "kitni battery", "power kitni")) -> 
                TaskResult(true, deviceInfoManager.getBatteryInfo())
            
            // ========== CALLS ==========
            containsAny(msg, listOf("call kar", "call karo", "phone kar", "phone karo", "call laga")) -> {
                val contact = extractContact(msg, listOf("call kar", "call karo", "phone kar", "phone karo", "call laga", "ko", "ka"))
                if (contact.isNotBlank()) {
                    TaskResult(true, phoneCallManager.makeCall(contact))
                } else {
                    TaskResult(true, "Kisko call karna hai yaar? Naam bata!")
                }
            }
            
            // ========== APPS ==========
            containsAny(msg, listOf("open kar", "khol", "launch", "chalu kar")) && !msg.contains("torch") -> {
                val appName = extractAppName(msg)
                if (appName.isNotBlank()) {
                    TaskResult(true, appLauncher.openApp(appName))
                } else {
                    TaskResult(true, "Konsi app kholni hai bata!")
                }
            }
            
            // ========== WHATSAPP ==========
            msg.contains("whatsapp") && containsAny(msg, listOf("bhej", "message", "msg", "text")) -> {
                val parts = msg.split(" ki ", " that ", " bolo ", " likh ")
                val contactPart = parts[0]
                val messagePart = if (parts.size > 1) parts[1] else "Hello"
                val contact = extractContact(contactPart, listOf("whatsapp", "pe", "par", "ko"))
                if (contact.isNotBlank()) {
                    TaskResult(true, whatsAppManager.sendWhatsAppMessage(contact, messagePart))
                } else {
                    TaskResult(true, "Kisko WhatsApp karna hai bata!")
                }
            }
            
            msg.contains("whatsapp") -> TaskResult(true, whatsAppManager.openWhatsApp())
            
            // ========== SMS ==========
            containsAny(msg, listOf("sms bhej", "message bhej", "sms kar")) -> {
                val parts = msg.split(" ki ", " that ", " bolo ", " likh ")
                val contactPart = parts[0]
                val messagePart = if (parts.size > 1) parts[1] else "Hello"
                val contact = extractContact(contactPart, listOf("sms bhej", "message bhej", "sms kar", "ko"))
                if (contact.isNotBlank()) {
                    TaskResult(true, smsManager.sendSMS(contact, messagePart))
                } else {
                    TaskResult(true, "Kisko SMS karna hai bata!")
                }
            }
            
            containsAny(msg, listOf("sms padh", "message padh", "last sms")) -> 
                TaskResult(true, smsManager.readRecentSMS())
            
            // ========== FLASHLIGHT ==========
            containsAny(msg, listOf("torch", "flashlight", "batti", "flash")) -> {
                val turnOn = containsAny(msg, listOf("on", "jala", "chalu", "start"))
                TaskResult(true, systemController.toggleFlashlight(turnOn))
            }
            
            // ========== WIFI ==========
            msg.contains("wifi") || msg.contains("wi-fi") -> {
                val turnOn = containsAny(msg, listOf("on", "chalu", "start", "enable"))
                TaskResult(true, systemController.toggleWifi(turnOn))
            }
            
            // ========== BLUETOOTH ==========
            msg.contains("bluetooth") -> {
                val turnOn = containsAny(msg, listOf("on", "chalu", "start", "enable"))
                TaskResult(true, systemController.toggleBluetooth(turnOn))
            }
            
            // ========== VOLUME ==========
            containsAny(msg, listOf("volume", "awaaz", "awaz", "sound")) -> {
                val action = when {
                    containsAny(msg, listOf("badha", "increase", "zyada", "up")) -> "up"
                    containsAny(msg, listOf("kam", "decrease", "low", "down")) -> "down"
                    containsAny(msg, listOf("mute", "silent", "chup")) -> "mute"
                    containsAny(msg, listOf("full", "max", "pura")) -> "max"
                    else -> "up"
                }
                TaskResult(true, systemController.controlVolume(action))
            }
            
            // ========== BRIGHTNESS ==========
            containsAny(msg, listOf("brightness", "roshni", "chamak")) -> {
                val action = when {
                    containsAny(msg, listOf("badha", "increase", "zyada")) -> "up"
                    containsAny(msg, listOf("kam", "decrease", "low")) -> "down"
                    containsAny(msg, listOf("full", "max", "pura")) -> "max"
                    else -> "up"
                }
                TaskResult(true, systemController.controlBrightness(action))
            }
            
            // ========== MUSIC ==========
            containsAny(msg, listOf("music chala", "gaana chala", "song chala", "play music", "play song")) -> 
                TaskResult(true, mediaController.play())
            
            containsAny(msg, listOf("music band", "gaana band", "music pause", "pause karo", "music rok")) -> 
                TaskResult(true, mediaController.pause())
            
            containsAny(msg, listOf("next song", "agla gaana", "next track", "skip")) -> 
                TaskResult(true, mediaController.next())
            
            // ========== ALARM ==========
            containsAny(msg, listOf("alarm laga", "alarm set")) -> {
                val time = alarmScheduler.parseTime(msg)
                if (time != null) {
                    TaskResult(true, alarmScheduler.setAlarm(time.first, time.second))
                } else {
                    TaskResult(true, "Kitne baje ka alarm lagau? Time bata!")
                }
            }
            
            // ========== CONTACT SEARCH ==========
            containsAny(msg, listOf("contact dhundh", "number dhundh", "number bata", "ka number")) -> {
                val name = extractContact(msg, listOf("contact dhundh", "number dhundh", "number bata", "ka number", "ka", "ki"))
                if (name.isNotBlank()) {
                    TaskResult(true, contactManager.getContactInfo(name))
                } else {
                    TaskResult(true, "Kiska number chahiye bata!")
                }
            }
            
            // ========== THANKS / BYE ==========
            containsAny(msg, listOf("thanks", "shukriya", "thank you", "dhanyawad")) -> 
                TaskResult(true, "Arre koi baat nahi yaar! Dost hu tera 💜")
            
            containsAny(msg, listOf("bye", "alvida", "chalti hu", "ja rahi", "bye bye")) -> 
                TaskResult(true, "Bye Vikram! Jab bhi zarurat ho, bulana! 👋")
            
            // ========== DEFAULT: Need AI ==========
            else -> TaskResult(false, "", requiresAI = true)
        }
    }

    // ========== HELPERS ==========
    
    private fun isGreeting(msg: String): Boolean {
        val greetings = listOf(
            "hi", "hello", "hey", "namaste", "namaskar",
            "kaise ho", "kaisi ho", "kya haal", "sup",
            "good morning", "good evening", "good night",
            "subah", "shaam"
        )
        return greetings.any { msg.contains(it) }
    }
    
    private fun getGreetingResponse(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetings = when {
            hour in 5..11 -> listOf(
                "Good morning Vikram! ☀️ Kaisa hai aaj?",
                "Subah ho gayi yaar! Uth gaya? 😄",
                "Namaste Vikram! Aaj ka din mast ho!"
            )
            hour in 12..16 -> listOf(
                "Kya haal hai Vikram? 😊",
                "Hey yaar! Lunch kar liya?",
                "Namaste! Kaise ho?"
            )
            hour in 17..20 -> listOf(
                "Good evening Vikram! 🌆",
                "Hey! Shaam ho gayi, kaisi rahi day?",
                "Namaste yaar! Chai piyoge? 😄"
            )
            else -> listOf(
                "Hey Vikram! Abhi tak jaag raha hai? 🌙",
                "Namaste! Late night coding? 💻",
                "Kya haal hai yaar? Raat ho gayi!"
            )
        }
        return greetings.random()
    }
    
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    private fun extractContact(text: String, keywords: List<String>): String {
        var result = text
        keywords.forEach { result = result.replace(it, " ") }
        return result.trim().replace(Regex("\\s+"), " ")
    }
    
    private fun extractAppName(msg: String): String {
        val keywords = listOf("open kar", "khol do", "khol", "launch", "chalu kar", "open", "kholna")
        var result = msg
        keywords.forEach { result = result.replace(it, " ") }
        return result.trim().replace(Regex("\\s+"), " ")
    }
}