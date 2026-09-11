package com.vikram.lena

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class LenaApplication : Application() {

    companion object {
        lateinit var instance: LenaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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
                description = "Lena is always ready to help"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            // Alert channel (for reminders)
            val alertChannel = NotificationChannel(
                "LenaAlertChannel",
                "Lena Alerts & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important reminders from Lena"
                enableVibration(true)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }
}