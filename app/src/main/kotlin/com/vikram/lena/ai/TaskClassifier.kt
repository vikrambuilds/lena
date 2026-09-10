package com.vikram.lena.ai

/**
 * User ne kya bola → kya karna hai
 * AI se baat karni hai ya phone ka koi task hai?
 */

enum class TaskType {
    CONVERSATION,       // Normal baat-cheet
    MAKE_CALL,          // Call karo
    ANSWER_CALL,        // Call pickup karo
    REJECT_CALL,        // Call reject karo
    OPEN_APP,           // App kholo
    SEND_SMS,           // SMS bhejo
    READ_SMS,           // SMS padho
    SEND_WHATSAPP,      // WhatsApp message
    SET_ALARM,          // Alarm lagao
    SET_REMINDER,       // Yaad dilao
    WIFI_TOGGLE,        // WiFi on/off
    BLUETOOTH_TOGGLE,   // Bluetooth on/off
    FLASHLIGHT_TOGGLE,  // Torch on/off
    VOLUME_CONTROL,     // Volume badhao/kam karo
    BRIGHTNESS_CONTROL, // Brightness set karo
    PLAY_MUSIC,         // Music chalao
    PAUSE_MUSIC,        // Music band karo
    NEXT_TRACK,         // Next song
    BATTERY_STATUS,     // Battery kitni hai
    TIME_DATE,          // Time/Date batao
    SEARCH_CONTACT,     // Contact dhundho
    TAKE_SCREENSHOT,    // Screenshot lo
    READ_NOTIFICATION,  // Notification padho
    UNKNOWN             // Samajh nahi aaya
}

data class ParsedTask(
    val type: TaskType,
    val params: Map<String, String> = emptyMap(),
    val originalText: String = ""
)

class TaskClassifier {

    fun classify(userMessage: String): ParsedTask {
        val msg = userMessage.lowercase().trim()

        return when {
            // ========== CALL TASKS ==========
            isCallTask(msg) -> parseCallTask(msg)
            isAnswerCallTask(msg) -> ParsedTask(TaskType.ANSWER_CALL)
            isRejectCallTask(msg) -> ParsedTask(TaskType.REJECT_CALL)

            // ========== APP TASKS ==========
            isOpenAppTask(msg) -> parseOpenAppTask(msg)

            // ========== SMS TASKS ==========
            isSendSMSTask(msg) -> parseSMSTask(msg)
            isReadSMSTask(msg) -> ParsedTask(TaskType.READ_SMS)

            // ========== WHATSAPP ==========
            isWhatsAppTask(msg) -> parseWhatsAppTask(msg)

            // ========== ALARM/REMINDER ==========
            isAlarmTask(msg) -> parseAlarmTask(msg)
            isReminderTask(msg) -> parseReminderTask(msg)

            // ========== SYSTEM CONTROLS ==========
            isWifiTask(msg) -> ParsedTask(
                TaskType.WIFI_TOGGLE,
                mapOf("action" to if (containsOnKeywords(msg)) "on" else "off")
            )
            isBluetoothTask(msg) -> ParsedTask(
                TaskType.BLUETOOTH_TOGGLE,
                mapOf("action" to if (containsOnKeywords(msg)) "on" else "off")
            )
            isFlashlightTask(msg) -> ParsedTask(
                TaskType.FLASHLIGHT_TOGGLE,
                mapOf("action" to if (containsOnKeywords(msg)) "on" else "off")
            )

            // ========== VOLUME ==========
            isVolumeTask(msg) -> parseVolumeTask(msg)

            // ========== BRIGHTNESS ==========
            isBrightnessTask(msg) -> parseBrightnessTask(msg)

            // ========== MUSIC ==========
            isPlayMusicTask(msg) -> ParsedTask(TaskType.PLAY_MUSIC)
            isPauseMusicTask(msg) -> ParsedTask(TaskType.PAUSE_MUSIC)
            isNextTrackTask(msg) -> ParsedTask(TaskType.NEXT_TRACK)

            // ========== INFO ==========
            isBatteryTask(msg) -> ParsedTask(TaskType.BATTERY_STATUS)
            isTimeTask(msg) -> ParsedTask(TaskType.TIME_DATE)

            // ========== CONTACT ==========
            isSearchContactTask(msg) -> parseContactTask(msg)

            // ========== NOTIFICATION ==========
            isNotificationTask(msg) -> ParsedTask(TaskType.READ_NOTIFICATION)

            // ========== DEFAULT: CONVERSATION ==========
            else -> ParsedTask(
                TaskType.CONVERSATION,
                originalText = userMessage
            )
        }
    }

    // ========== DETECTION METHODS ==========

    private fun isCallTask(msg: String): Boolean {
        val keywords = listOf(
            "call kar", "call karo", "call laga",
            "phone kar", "phone karo", "dial kar",
            "ko call", "ko phone", "call to",
            "ring kar", "baat kara", "baat karwa"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isAnswerCallTask(msg: String): Boolean {
        val keywords = listOf(
            "call utha", "call pick", "phone utha",
            "receive kar", "answer kar", "call attend",
            "phone attend", "utha le", "pick up"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isRejectCallTask(msg: String): Boolean {
        val keywords = listOf(
            "call kat", "call reject", "phone kat",
            "decline kar", "call cut", "mat utha",
            "band kar call", "reject kar"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isOpenAppTask(msg: String): Boolean {
        val keywords = listOf(
            "open kar", "khol", "launch kar",
            "start kar", "chalu kar", "open",
            "kholna", "khol do", "khol de"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isSendSMSTask(msg: String): Boolean {
        val keywords = listOf(
            "sms bhej", "message bhej", "sms kar",
            "text kar", "msg bhej", "message send"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isReadSMSTask(msg: String): Boolean {
        val keywords = listOf(
            "sms padh", "message padh", "sms dikha",
            "message dikha", "kya message", "msg padh",
            "last sms", "last message"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isWhatsAppTask(msg: String): Boolean {
        val keywords = listOf(
            "whatsapp", "whats app", "wa pe",
            "whatsapp bhej", "whatsapp message", "whatsapp kar"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isAlarmTask(msg: String): Boolean {
        val keywords = listOf(
            "alarm laga", "alarm set", "alarm baja",
            "jagana", "jaga dena", "alarm kar"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isReminderTask(msg: String): Boolean {
        val keywords = listOf(
            "yaad dila", "remind kar", "reminder laga",
            "reminder set", "mat bhulna", "yaad rakh"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isWifiTask(msg: String): Boolean {
        return msg.contains("wifi") || msg.contains("wi-fi") || msg.contains("waifi")
    }

    private fun isBluetoothTask(msg: String): Boolean {
        return msg.contains("bluetooth") || msg.contains("bt ") || 
               msg.contains("blue tooth")
    }

    private fun isFlashlightTask(msg: String): Boolean {
        val keywords = listOf(
            "torch", "flashlight", "flash",
            "light on", "light off", "torch jala",
            "batti", "flash jala"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isVolumeTask(msg: String): Boolean {
        val keywords = listOf(
            "volume", "awaaz", "awaz", "sound",
            "vol ", "bol", "mute", "silent"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isBrightnessTask(msg: String): Boolean {
        val keywords = listOf(
            "brightness", "bright", "screen light",
            "roshni", "chamak"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isPlayMusicTask(msg: String): Boolean {
        val keywords = listOf(
            "music chala", "gaana chala", "song chala",
            "play music", "play song", "gaana sun"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isPauseMusicTask(msg: String): Boolean {
        val keywords = listOf(
            "music band", "gaana band", "song band",
            "pause music", "stop music", "music ruk",
            "gaana rok"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isNextTrackTask(msg: String): Boolean {
        val keywords = listOf(
            "next song", "agla gaana", "next track",
            "skip kar", "next chala"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isBatteryTask(msg: String): Boolean {
        val keywords = listOf(
            "battery", "charge", "kitni battery",
            "power", "charging"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isTimeTask(msg: String): Boolean {
        val keywords = listOf(
            "time", "samay", "waqt", "kitne baje",
            "date", "tarikh", "din kya", "kya din hai"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isSearchContactTask(msg: String): Boolean {
        val keywords = listOf(
            "contact dhundh", "contact search", "number dhundh",
            "number bata", "ka number", "contact dikha"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun isNotificationTask(msg: String): Boolean {
        val keywords = listOf(
            "notification padh", "notification dikha",
            "kya notification", "notifications", "notify"
        )
        return keywords.any { msg.contains(it) }
    }

    private fun containsOnKeywords(msg: String): Boolean {
        val onWords = listOf("on", "chalu", "start", "jala", "enable", "karo on")
        return onWords.any { msg.contains(it) }
    }

    // ========== PARSING METHODS ==========

    private fun parseCallTask(msg: String): ParsedTask {
        // "Mummy ko call karo" → contact = "Mummy"
        val contactName = extractContactName(msg, listOf(
            "call kar", "call karo", "call laga", "phone kar",
            "phone karo", "ko call", "ko phone"
        ))
        return ParsedTask(
            TaskType.MAKE_CALL,
            mapOf("contact" to contactName),
            msg
        )
    }

    private fun parseOpenAppTask(msg: String): ParsedTask {
        val appName = extractAfterKeyword(msg, listOf(
            "open kar", "khol", "launch kar", "start kar",
            "chalu kar", "khol do", "khol de"
        ))
        return ParsedTask(
            TaskType.OPEN_APP,
            mapOf("appName" to appName),
            msg
        )
    }

    private fun parseSMSTask(msg: String): ParsedTask {
        // "Rahul ko message bhej ki main aa raha hu"
        val parts = msg.split("ki ", "that ", "bolo ", "likh ")
        val contactPart = parts[0]
        val messagePart = if (parts.size > 1) parts[1] else ""
        val contact = extractContactName(contactPart, listOf(
            "sms bhej", "message bhej", "ko", "sms kar"
        ))
        return ParsedTask(
            TaskType.SEND_SMS,
            mapOf("contact" to contact, "message" to messagePart),
            msg
        )
    }

    private fun parseWhatsAppTask(msg: String): ParsedTask {
        val parts = msg.split("ki ", "that ", "bolo ", "likh ")
        val contactPart = parts[0]
        val messagePart = if (parts.size > 1) parts[1] else ""
        val contact = extractContactName(contactPart, listOf(
            "whatsapp", "ko", "pe", "par"
        ))
        return ParsedTask(
            TaskType.SEND_WHATSAPP,
            mapOf("contact" to contact, "message" to messagePart),
            msg
        )
    }

    private fun parseAlarmTask(msg: String): ParsedTask {
        // "7 baje alarm laga do"
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(baje|am|pm)?")
        val match = timeRegex.find(msg)
        val hour = match?.groupValues?.get(1) ?: "7"
        val minute = match?.groupValues?.get(2) ?: "0"
        return ParsedTask(
            TaskType.SET_ALARM,
            mapOf("hour" to hour, "minute" to minute),
            msg
        )
    }

    private fun parseReminderTask(msg: String): ParsedTask {
        val content = extractAfterKeyword(msg, listOf(
            "yaad dila", "remind kar", "reminder laga"
        ))
        return ParsedTask(
            TaskType.SET_REMINDER,
            mapOf("content" to content),
            msg
        )
    }

    private fun parseVolumeTask(msg: String): ParsedTask {
        val action = when {
            msg.contains("badha") || msg.contains("increase") || 
            msg.contains("zyada") || msg.contains("up") -> "up"
            msg.contains("kam") || msg.contains("decrease") || 
            msg.contains("low") || msg.contains("down") -> "down"
            msg.contains("mute") || msg.contains("silent") || 
            msg.contains("chup") -> "mute"
            msg.contains("full") || msg.contains("max") || 
            msg.contains("pura") -> "max"
            else -> "up"
        }
        return ParsedTask(
            TaskType.VOLUME_CONTROL,
            mapOf("action" to action),
            msg
        )
    }

    private fun parseBrightnessTask(msg: String): ParsedTask {
        val action = when {
            msg.contains("badha") || msg.contains("increase") || 
            msg.contains("zyada") -> "up"
            msg.contains("kam") || msg.contains("decrease") || 
            msg.contains("low") -> "down"
            msg.contains("full") || msg.contains("max") || 
            msg.contains("pura") -> "max"
            msg.contains("min") || msg.contains("dim") -> "min"
            else -> "up"
        }
        return ParsedTask(
            TaskType.BRIGHTNESS_CONTROL,
            mapOf("action" to action),
            msg
        )
    }

    private fun parseContactTask(msg: String): ParsedTask {
        val name = extractAfterKeyword(msg, listOf(
            "contact dhundh", "number dhundh", "number bata", "ka number"
        ))
        return ParsedTask(
            TaskType.SEARCH_CONTACT,
            mapOf("name" to name),
            msg
        )
    }

    // ========== HELPER METHODS ==========

    private fun extractContactName(msg: String, keywords: List<String>): String {
        var cleaned = msg
        keywords.forEach { cleaned = cleaned.replace(it, "") }
        cleaned = cleaned.replace("ko", "").replace("ka", "")
            .replace("ki", "").replace("ke", "")
        return cleaned.trim().ifEmpty { "unknown" }
    }

    private fun extractAfterKeyword(msg: String, keywords: List<String>): String {
        keywords.forEach { keyword ->
            if (msg.contains(keyword)) {
                val index = msg.indexOf(keyword) + keyword.length
                return msg.substring(index).trim()
            }
        }
        return msg
    }
}