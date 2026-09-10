package com.vikram.lena

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.vikram.lena.data.PreferencesManager

class LenaApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    companion object {
        lateinit var instance: LenaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Main service channel
            val serviceChannel = NotificationChannel(
                "LenaServiceChannel",
                "Lena Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lena is always listening for your voice"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(serviceChannel)

            // Alert channel (for reminders, greetings)
            val alertChannel = NotificationChannel(
                "LenaAlertChannel",
                "Lena Alerts & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts from Lena"
                enableVibration(true)
            }
            manager.createNotificationChannel(alertChannel)

            // Chat channel
            val chatChannel = NotificationChannel(
                "LenaChatChannel",
                "Lena Chat Responses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lena's chat responses"
            }
            manager.createNotificationChannel(chatChannel)
        }
    }
}