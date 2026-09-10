package com.vikram.lena.core

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        var recentNotifications = mutableListOf<NotificationInfo>()
        const val MAX_NOTIFICATIONS = 20
    }

    data class NotificationInfo(
        val appName: String,
        val title: String,
        val text: String,
        val timestamp: Long
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val appName = sbn.packageName

        val info = NotificationInfo(
            appName = getAppName(appName),
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        recentNotifications.add(0, info)
        if (recentNotifications.size > MAX_NOTIFICATIONS) {
            recentNotifications = recentNotifications
                .take(MAX_NOTIFICATIONS).toMutableList()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /** Get recent notifications as readable text */
    fun getNotificationSummary(): String {
        return if (recentNotifications.isNotEmpty()) {
            val summary = recentNotifications.take(5).joinToString("\n") {
                "📱 ${it.appName}: ${it.title} - ${it.text}"
            }
            "Tere recent notifications:\n\n$summary"
        } else {
            "Koi nayi notification nahi hai yaar!"
        }
    }
}