package com.vikram.lena.ai

import android.content.Context
import com.vikram.lena.automation.*

/**
 * Smart Task Executor v2.2
 * - Fuzzy keyword matching
 * - Better contact extraction
 * - Handles typos and speech recognition errors
 * - Multi-language support (Hindi + English + Hinglish)
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
        val handled: Boolean,
        val response: String,
        val requiresAI: Boolean = false
    )

    fun execute(userMessage: String): TaskResult {
        val msg = userMessage.lowercase().trim()
        
        // Remove wake word if present in command
        var cleanMsg = msg
        listOf("hey lena", "ok lena", "lena", "leena", "lina", "arre lena", "arey lena").forEach {
            cleanMsg = cleanMsg.replace(it, "").trim()
        }
        if (cleanMsg.isBlank()) cleanMsg = msg

        return when {
            // ========== TORCH / FLASHLIGHT ==========
            hasAny(cleanMsg, listOf("torch", "flashlight", "batti", "light", "flash", "tourch", "torc")) -> {
                val turnOn = !hasAny(cleanMsg, listOf("off", "band", "bandh", "close", "bujha"))
                TaskResult(true, systemController.toggleFlashlight(turnOn))
            }
            
            // ========== CALLS ==========
            hasAny(cleanMsg, listOf("call", "phone", "dial", "ring")) && 
            !hasAny(cleanMsg, listOf("logs", "history", "list")) -> {
                val contact = extractContactForCall(cleanMsg)
                if (contact.isNotBlank() && contact.length > 1) {
                    TaskResult(true, phoneCallManager.makeCall(contact))
                } else {
                    TaskResult(true, "Kisko call karna hai yaar? Naam bata!")
                }
            }
            
            // ========== APPS ==========
            hasAny(cleanMsg, listOf("open", "khol", "kholo", "launch", "chalu", "start")) && 
            !hasAny(cleanMsg, listOf("torch", "flashlight", "wifi", "bluetooth", "app khol karo")) -> {
                val appName = extractAppName(cleanMsg)
                if (appName.isNotBlank() && appName.length > 1) {
                    TaskResult(true, appLauncher.openApp(appName))
                } else {
                    TaskResult(true, "Konsi app kholni hai? Naam bata!")
                }
            }
            
            // Direct app names (without "open" keyword)
            hasAny(cleanMsg, listOf("youtube", "whatsapp", "instagram", "facebook", "chrome", "gmail")) &&
            !hasAny(cleanMsg, listOf("message", "bhej", "call")) -> {
                val appName = extractDirectAppName(cleanMsg)
                if (appName.isNotBlank()) {
                    TaskResult(true, appLauncher.openApp(appName))
                } else {
                    TaskResult(false, "", requiresAI = true)
                }
            }
            
            // ========== WHATSAPP ==========
            cleanMsg.contains("whatsapp") && hasAny(cleanMsg, listOf("bhej", "message", "msg", "text", "send")) -> {
                val (contact, message) = extractContactAndMessage(cleanMsg, "whatsapp")
                if (contact.isNotBlank()) {
                    TaskResult(true, whatsAppManager.sendWhatsAppMessage(contact, message.ifBlank { "Hello!" }))
                } else {
                    TaskResult(true, "Kisko WhatsApp bhejna hai?")
                }
            }
            
            cleanMsg.contains("whatsapp") -> TaskResult(true, whatsAppManager.openWhatsApp())
            
            // ========== SMS ==========
            hasAny(cleanMsg, listOf("sms bhej", "message bhej", "sms send", "text send")) -> {
                val (contact, message) = extractContactAndMessage(cleanMsg, "sms")
                if (contact.isNotBlank()) {
                    TaskResult(true, smsManager.sendSMS(contact, message.ifBlank { "Hello!" }))
                } else {
                    TaskResult(true, "Kisko SMS karna hai?")
                }
            }
            
            hasAny(cleanMsg, listOf("sms padh", "message padh", "last sms", "sms dikha")) -> 
                TaskResult(true, smsManager.readRecentSMS())
            
            // ========== TIME & DATE ==========
            hasAny(cleanMsg, listOf("time", "samay", "kitne baje", "waqt", "kya baja", "kya baje", "kya time")) -> 
                TaskResult(true, deviceInfoManager.getTimeAndDate())
            
            hasAny(cleanMsg, listOf("date", "tarikh", "din kya", "aaj kya din", "aaj ki date")) -> 
                TaskResult(true, deviceInfoManager.getTimeAndDate())
            
            // ========== BATTERY ==========
            hasAny(cleanMsg, listOf("battery", "charge", "kitni battery", "power kitni", "phone battery")) -> 
                TaskResult(true, deviceInfoManager.getBatteryInfo())
            
            // ========== WIFI ==========
            cleanMsg.contains("wifi") || cleanMsg.contains("wi-fi") || cleanMsg.contains("wifi") -> {
                val turnOn = !hasAny(cleanMsg, listOf("off", "band", "bandh", "disable"))
                TaskResult(true, systemController.toggleWifi(turnOn))
            }
            
            // ========== BLUETOOTH ==========
            cleanMsg.contains("bluetooth") -> {
                val turnOn = !hasAny(cleanMsg, listOf("off", "band", "bandh", "disable"))
                TaskResult(true, systemController.toggleBluetooth(turnOn))
            }
            
            // ========== VOLUME ==========
            hasAny(cleanMsg, listOf("volume", "awaaz", "awaz", "sound", "vol")) -> {
                val action = when {
                    hasAny(cleanMsg, listOf("badha", "increase", "zyada", "up", "high")) -> "up"
                    hasAny(cleanMsg, listOf("kam", "decrease", "low", "down", "kam kar")) -> "down"
                    hasAny(cleanMsg, listOf("mute", "silent", "chup", "band")) -> "mute"
                    hasAny(cleanMsg, listOf("full", "max", "pura", "maximum")) -> "max"
                    else -> "up"
                }
                TaskResult(true, systemController.controlVolume(action))
            }
            
            // ========== BRIGHTNESS ==========
            hasAny(cleanMsg, listOf("brightness", "roshni", "chamak", "screen light")) -> {
                val action = when {
                    hasAny(cleanMsg, listOf("badha", "increase", "zyada", "up")) -> "up"
                    hasAny(cleanMsg, listOf("kam", "decrease", "low", "down")) -> "down"
                    hasAny(cleanMsg, listOf("full", "max", "pura")) -> "max"
                    else -> "up"
                }
                TaskResult(true, systemController.controlBrightness(action))
            }
            
            // ========== MUSIC ==========
            hasAny(cleanMsg, listOf("music chala", "gaana chala", "song chala", "play music", "play song", "music play")) -> 
                TaskResult(true, mediaController.play())
            
            hasAny(cleanMsg, listOf("music band", "gaana band", "music pause", "pause karo", "music rok", "stop music")) -> 
                TaskResult(true, mediaController.pause())
            
            hasAny(cleanMsg, listOf("next song", "agla gaana", "next track", "skip", "next music")) -> 
                TaskResult(true, mediaController.next())
            
            // ========== ALARM ==========
            hasAny(cleanMsg, listOf("alarm laga", "alarm set", "alarm lag", "wake me")) -> {
                val time = alarmScheduler.parseTime(cleanMsg)
                if (time != null) {
                    TaskResult(true, alarmScheduler.setAlarm(time.first, time.second))
                } else {
                    TaskResult(true, "Kitne baje ka alarm lagau? Time bata!")
                }
            }
            
            // ========== CONTACT SEARCH ==========
            hasAny(cleanMsg, listOf("contact dhundh", "number dhundh", "number bata", "ka number", "contact search")) -> {
                val name = extractSimpleName(cleanMsg, listOf("contact dhundh", "number dhundh", "number bata", "ka number", "ka", "ki", "contact"))
                if (name.isNotBlank()) {
                    TaskResult(true, contactManager.getContactInfo(name))
                } else {
                    TaskResult(true, "Kiska number chahiye bata!")
                }
            }
            
            // ========== GREETINGS ==========
            isGreeting(cleanMsg) -> TaskResult(true, getGreetingResponse())
            
            // ========== THANKS / BYE ==========
            hasAny(cleanMsg, listOf("thanks", "shukriya", "thank you", "dhanyawad", "thanku")) -> 
                TaskResult(true, "Arre koi baat nahi yaar! Dost hu tera 💜")
            
            hasAny(cleanMsg, listOf("bye", "alvida", "chalti hu", "ja rahi", "bye bye", "chal bye")) -> 
                TaskResult(true, "Bye Vikram! Jab bhi zarurat ho, bulana! 👋")
            
            // ========== JOKES ==========
            hasAny(cleanMsg, listOf("joke", "chutkula", "hansa", "joke suna")) -> 
                TaskResult(true, getRandomJoke())
            
            // ========== MOTIVATIONAL ==========
            hasAny(cleanMsg, listOf("motivate", "motivation", "himmat", "encourage", "quote")) -> 
                TaskResult(true, getMotivationalQuote())
            
            // ========== DEFAULT: Need AI ==========
            else -> TaskResult(false, "", requiresAI = true)
        }
    }

    // ========== SMART EXTRACTORS ==========
    
    private fun extractContactForCall(msg: String): String {
        // Common patterns:
        // "mummy ko call karo" → mummy
        // "call papa" → papa
        // "papa ko phone lagao" → papa
        // "rahul ko call kar" → rahul
        
        var text = msg
        val removeWords = listOf(
            "call karo", "call kar", "call laga", "call lagao",
            "phone karo", "phone kar", "phone laga", "phone lagao",
            "dial karo", "dial kar", "ring karo",
            "ko call", "ko phone", "ko dial", "ko ring",
            "call", "phone", "dial", "ring",
            "please", "yaar", "abhi", "jaldi",
            " ko ", " ka ", " ki ", " ke "
        )
        
        removeWords.forEach { text = text.replace(it, " ") }
        return text.trim().replace(Regex("\\s+"), " ")
    }
    
    private fun extractAppName(msg: String): String {
        var text = msg
        val removeWords = listOf(
            "app khol karo", "app kholna", 
            "open karo", "open kar", "open",
            "khol do", "khol de", "khol", "kholo",
            "launch karo", "launch kar", "launch",
            "chalu karo", "chalu kar", "chalu",
            "start karo", "start kar", "start",
            "app", "ko", "please", "yaar", "abhi"
        )
        
        removeWords.forEach { text = text.replace(it, " ") }
        return text.trim().replace(Regex("\\s+"), " ")
    }
    
    private fun extractDirectAppName(msg: String): String {
        val apps = listOf(
            "youtube", "whatsapp", "instagram", "facebook", 
            "chrome", "gmail", "twitter", "telegram", "snapchat",
            "spotify", "netflix", "amazon", "flipkart", "paytm",
            "phonepe", "gpay", "maps", "camera", "gallery"
        )
        
        return apps.firstOrNull { msg.contains(it) } ?: ""
    }
    
    private fun extractContactAndMessage(msg: String, appType: String): Pair<String, String> {
        // "rahul ko whatsapp bhej ki main aa raha hu"
        // → contact: rahul, message: main aa raha hu
        
        val separators = listOf(" ki ", " that ", " bolo ", " likh ", " message ", " msg ")
        var contactPart = msg
        var messagePart = ""
        
        for (sep in separators) {
            if (msg.contains(sep)) {
                val parts = msg.split(sep, limit = 2)
                contactPart = parts[0]
                messagePart = if (parts.size > 1) parts[1] else ""
                break
            }
        }
        
        // Clean contact part
        val removeWords = listOf(
            "whatsapp bhej", "whatsapp send", "whatsapp kar", "whatsapp",
            "sms bhej", "sms send", "sms kar", "sms",
            "message bhej", "message send", "text bhej",
            "ko", "ka", "ki", "pe", "par", "please", "yaar"
        )
        
        var contact = contactPart
        removeWords.forEach { contact = contact.replace(it, " ") }
        contact = contact.trim().replace(Regex("\\s+"), " ")
        
        return Pair(contact, messagePart.trim())
    }
    
    private fun extractSimpleName(msg: String, keywords: List<String>): String {
        var text = msg
        keywords.forEach { text = text.replace(it, " ") }
        return text.trim().replace(Regex("\\s+"), " ")
    }
    
    private fun hasAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    private fun isGreeting(msg: String): Boolean {
        val greetings = listOf(
            "hi", "hello", "hey", "namaste", "namaskar",
            "kaise ho", "kaisi ho", "kya haal", "sup",
            "good morning", "good evening", "good night",
            "subah bakhair", "shaam bakhair"
        )
        return greetings.any { msg.contains(it) }
    }
    
    private fun getGreetingResponse(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetings = when {
            hour in 5..11 -> listOf(
                "Good morning Vikram! ☀️ Aaj ka din mast ho!",
                "Subah ho gayi yaar! Uth gaya? Chai peeni hai? ☕",
                "Namaste Vikram! Ready for the day? 😊"
            )
            hour in 12..16 -> listOf(
                "Kya haal hai Vikram? 😊 Lunch kar liya?",
                "Hey yaar! Kya kar raha hai?",
                "Namaste! Din kaisa ja raha hai?"
            )
            hour in 17..20 -> listOf(
                "Good evening Vikram! 🌆 Kaisa raha din?",
                "Hey! Shaam ho gayi, thak gaya?",
                "Namaste yaar! Chai break?"
            )
            else -> listOf(
                "Hey Vikram! Late night coding? 💻",
                "Namaste! Abhi tak jaag raha hai?",
                "Kya haal hai? Neend nahi aa rahi kya?"
            )
        }
        return greetings.random()
    }
    
    private fun getRandomJoke(): String {
        val jokes = listOf(
            "Teacher: Vikram, tumhare paper mein tumhare bhai jaisi likhawat hai. Vikram: Sir, hum dono ek hi pen use karte hain! 😂",
            "Programmer ki wife: Bread lekar aana, agar ande milen toh ek le lena. Programmer 12 breads laata hai. Wife: Kyu? Programmer: Ande mil gaye the! 🤣",
            "CSE student ka pyar: Mujhe tumse infinite loop jitna pyar hai! Girl: Break statement kab lagega? 😄",
            "Vikram: Yaar, mujhe rat ko sapna aya ki main lottery jeeta! Friend: Congrats! Kitne mile? Vikram: Sapna tha yaar! 🤪"
        )
        return jokes.random()
    }
    
    private fun getMotivationalQuote(): String {
        val quotes = listOf(
            "Vikram yaar, tu bahut talented hai! Bas apne aap pe bharosa rakh! 💪",
            "Har din ek nayi shuruat hai. Aaj se mehnat shuru kar, kal ka superstar tu hoga! 🌟",
            "5th sem hai bhai, ab focus kar. Placement ke liye ready ho ja! 🚀",
            "Failure success ki seedhi hai. Tu bhi kar sakta hai, main hu na tere saath! ❤️",
            "CSE mein ho, world change karne wale ho. Chalo, code likhna shuru karo! 💻"
        )
        return quotes.random()
    }
}