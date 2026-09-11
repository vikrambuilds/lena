package com.vikram.lena.automation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

class SystemController(private val context: Context) {

    private var isFlashOn = false

    // ========== FLASHLIGHT / TORCH ==========
    fun toggleFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            
            // Find camera with flash support
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return "Yaar tere phone mein flash nahi mila! 😅"
            }

            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashOn = turnOn
            
            if (turnOn) "Torch jala di! 🔦✨" else "Torch band kar di! 🔦"
        } catch (e: CameraAccessException) {
            "Camera busy hai yaar! Camera band karke try karo."
        } catch (e: Exception) {
            "Torch mein problem: ${e.message?.take(30)}"
        }
    }

    // ========== WIFI ==========
    fun toggleWifi(turnOn: Boolean): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "WiFi settings khol di, wahan se ${if (turnOn) "ON" else "OFF"} kar lo! 📶"
            } else {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = turnOn
                "WiFi ${if (turnOn) "ON ✅" else "OFF ❌"} kar diya!"
            }
        } catch (e: Exception) {
            "WiFi change karne mein problem aa rahi hai!"
        }
    }

    // ========== BLUETOOTH ==========
    fun toggleBluetooth(turnOn: Boolean): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter

            if (adapter == null) {
                return "Tere phone mein Bluetooth support nahi hai!"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Bluetooth settings khol di hai! 🔵"
            } else {
                @Suppress("DEPRECATION")
                if (turnOn) adapter.enable() else adapter.disable()
                "Bluetooth ${if (turnOn) "ON ✅" else "OFF ❌"} kar diya!"
            }
        } catch (e: Exception) {
            "Bluetooth toggle nahi ho paya: ${e.message?.take(30)}"
        }
    }

    // ========== VOLUME ==========
    fun controlVolume(action: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        return when (action) {
            "up" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                "Volume badha di! 🔊 (${currentVolume + 1}/$maxVolume)"
            }
            "down" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                "Volume kam kar di! 🔉 (${currentVolume - 1}/$maxVolume)"
            }
            "mute" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                "Mute kar diya! 🔇"
            }
            "max" -> {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI
                )
                "Volume full kar di! 🔊🔊"
            }
            else -> "Volume badhau ya kam karu?"
        }
    }

    // ========== BRIGHTNESS ==========
    fun controlBrightness(action: String): String {
        return try {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return "Pehle brightness permission allow kar do Settings mein!"
            }

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            val newBrightness = when (action) {
                "up" -> minOf(currentBrightness + 50, 255)
                "down" -> maxOf(currentBrightness - 50, 10)
                "max" -> 255
                "min" -> 10
                else -> 128
            }

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            "Brightness ${action} kar di! 💡"
        } catch (e: Exception) {
            "Brightness badalne mein issue aaya!"
        }
    }

    // ========== MUSIC CONTROL ==========
    fun controlMedia(action: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return when (action) {
            "play", "pause" -> {
                val keyEvent = if (action == "play")
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                else
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE

                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEvent)
                )
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEvent)
                )
                if (action == "play") "Music play kar diya! 🎵" else "Music pause kar diya! ⏸️"
            }
            "next" -> {
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                )
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                )
                "Agla gaana laga rahi hu! ⏭️"
            }
            else -> "Music control ready hai!"
        }
    }

    // ========== ALARM ==========
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
            "Alarm set kar diya $hour:${String.format("%02d", minute)} par! ⏰"
        } catch (e: Exception) {
            "Alarm set karne mein issue aaya!"
        }
    }

    // ========== BATTERY STATUS ==========
    fun getBatteryStatus(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        return "Battery $batteryLevel% hai" + if (isCharging) " aur charging chal rahi hai ⚡" else " 🔋"
    }

    // ========== TIME & DATE ==========
    fun getTimeDate(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("hi", "IN"))
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("hi", "IN"))
        return "Abhi ${timeFormat.format(now)} baj rahe hain ⏰\nAaj ${dateFormat.format(now)} hai 📅"
    }
}