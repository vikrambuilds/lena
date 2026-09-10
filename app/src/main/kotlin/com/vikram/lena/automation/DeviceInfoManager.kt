package com.vikram.lena.automation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.*

class DeviceInfoManager(private val context: Context) {

    /** Get battery info */
    fun getBatteryInfo(): String {
        val batteryManager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager

        val level = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
        val isCharging = batteryManager.isCharging
        val temperature = getBatteryTemperature()

        val emoji = when {
            level > 80 -> "🔋"
            level > 50 -> "🔋"
            level > 20 -> "🪫"
            else -> "⚠️"
        }

        var info = "Battery $level% hai $emoji"
        if (isCharging) info += "\nCharging chal rahi hai ⚡"
        if (temperature > 0) info += "\nTemperature: ${temperature}°C"

        // Battery health warning
        if (level <= 15 && !isCharging) {
            info += "\n\n⚠️ Yaar battery bahut kam hai! Charge kar le jaldi!"
        }

        return info
    }

    private fun getBatteryTemperature(): Float {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10f
    }

    /** Get current time and date */
    fun getTimeAndDate(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("hi", "IN"))
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN"))
        val dayFormat = SimpleDateFormat("EEEE", Locale("hi", "IN"))

        return "Abhi ${timeFormat.format(now)} baj rahe hain ⏰\n" +
                "Aaj ${dayFormat.format(now)}, ${dateFormat.format(now)} hai 📅"
    }

    /** Get network info */
    fun getNetworkInfo(): String {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return when {
            capabilities == null -> "Internet nahi chal raha yaar! ❌"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                "WiFi se connected hai! 📶"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                "Mobile data se connected hai! 📱"
            else -> "Kuch toh internet chal raha hai!"
        }
    }

    /** Get RAM info */
    fun getRAMInfo(): String {
        val activityManager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRAM = memoryInfo.totalMem / (1024 * 1024)
        val availRAM = memoryInfo.availMem / (1024 * 1024)
        val usedRAM = totalRAM - availRAM

        return "RAM: ${usedRAM}MB used / ${totalRAM}MB total\n" +
                "Free: ${availRAM}MB"
    }

    /** Get storage info */
    fun getStorageInfo(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = totalBytes - freeBytes

        val totalGB = totalBytes / (1024.0 * 1024 * 1024)
        val freeGB = freeBytes / (1024.0 * 1024 * 1024)
        val usedGB = usedBytes / (1024.0 * 1024 * 1024)

        return "Storage: %.1f GB used / %.1f GB total\nFree: %.1f GB".format(
            usedGB, totalGB, freeGB
        )
    }

    /** Get device info */
    fun getDeviceInfo(): String {
        return "Phone: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                getBatteryInfo() + "\n" +
                getNetworkInfo() + "\n" +
                getRAMInfo()
    }

    /** Get complete status summary */
    fun getFullStatus(): String {
        return """
${getTimeAndDate()}

🔋 ${getBatteryInfo()}

📶 ${getNetworkInfo()}

💾 ${getRAMInfo()}

📦 ${getStorageInfo()}

📱 Phone: ${Build.MANUFACTURER} ${Build.MODEL}
🤖 Android ${Build.VERSION.RELEASE}
        """.trimIndent()
    }
}