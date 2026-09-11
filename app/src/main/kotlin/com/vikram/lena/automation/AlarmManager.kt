package com.vikram.lena.automation

import android.app.AlarmManager as AndroidAlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.*

class AlarmScheduler(private val context: Context) {

    /** Set alarm using system alarm app */
    fun setAlarm(hour: Int, minute: Int, label: String = "Lena Alarm"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val period = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            "Alarm set kar diya $displayHour:${String.format("%02d", minute)} $period pe! ⏰"
        } catch (e: Exception) {
            "Alarm set karne mein problem aa gayi: ${e.message}"
        }
    }

    /** Set timer (countdown) */
    fun setTimer(minutes: Int, label: String = "Lena Timer"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Timer set kar diya $minutes minutes ka! ⏱️"
        } catch (e: Exception) {
            "Timer set karne mein problem aa gayi!"
        }
    }

    /** Show all alarms */
    fun showAlarms(): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Tere saare alarms dikha rahi hu! ⏰"
        } catch (e: Exception) {
            "Alarms dikhane mein problem aa gayi!"
        }
    }

    /** Set reminder with notification after delay */
    fun setReminder(message: String, delayMinutes: Int = 30): String {
        return try {
            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AndroidAlarmManager

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminder_message", message)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

            val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

            alarmManager.setExactAndAllowWhileIdle(
                AndroidAlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            "Reminder set kar diya! $delayMinutes minutes baad yaad dilaaungi: \"$message\" 📝"
        } catch (e: Exception) {
            "Reminder set karne mein problem aa gayi: ${e.message}"
        }
    }

    /** Parse time from user message */
    fun parseTime(message: String): Pair<Int, Int>? {
        val msg = message.lowercase()

        val numberWords = mapOf(
            "ek" to 1, "do" to 2, "teen" to 3, "char" to 4,
            "paanch" to 5, "che" to 6, "saat" to 7, "aath" to 8,
            "nau" to 9, "das" to 10, "gyarah" to 11, "barah" to 12
        )

        var hour: Int? = null
        var minute = 0

        val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
        val timeMatch = timeRegex.find(msg)
        if (timeMatch != null) {
            hour = timeMatch.groupValues[1].toIntOrNull()
            minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        }

        if (hour == null) {
            val bajeRegex = Regex("(\\d{1,2})\\s*baje")
            val bajeMatch = bajeRegex.find(msg)
            if (bajeMatch != null) {
                hour = bajeMatch.groupValues[1].toIntOrNull()
            }
        }

        if (hour == null) {
            for ((word, value) in numberWords) {
                if (msg.contains(word)) {
                    hour = value
                    break
                }
            }
        }

        if (hour == null) return null

        val isNight = msg.contains("raat") || msg.contains("night")
        val isEvening = msg.contains("shaam") || msg.contains("evening")
        val isAfternoon = msg.contains("dopahar") || msg.contains("afternoon")

        if (hour in 1..11) {
            if (isNight && hour < 12) hour += 12
            if (isEvening && hour < 12) hour += 12
            if (isAfternoon && hour < 12) hour += 12
        }

        return Pair(hour, minute)
    }
}