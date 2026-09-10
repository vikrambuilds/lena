package com.vikram.lena.automation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
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

    // ========== WIFI ==========
    fun toggleWifi(turnOn: Boolean): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ needs settings panel
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "WiFi settings khol rahi hu, ${if (turnOn) "ON" else "OFF"} kar de! 📶"
            } else {
                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = turnOn
                "WiFi ${if (turnOn) "ON ✅" else "OFF ❌"} kar diya!"
            }
        } catch (e: Exception) {
            "WiFi change karne mein problem aa gayi yaar!"
        }
    }

    // ========== BLUETOOTH ==========
    fun toggleBluetooth(turnOn: Boolean): String {
        return try {
            val bluetoothManager = context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager
            val adapter = bluetoothManager.adapter

            if (adapter == null) {
                return "Yaar tere phone mein Bluetooth nahi hai!"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Bluetooth settings khol rahi hu! 🔵"
            } else {
                @Suppress("DEPRECATION")
                if (turnOn) adapter.enable() else adapter.disable()
                "Bluetooth ${if (turnOn) "ON ✅" else "OFF ❌"} kar diya!"
            }
        } catch (e: Exception) {
            "Bluetooth toggle mein problem: ${e.message}"
        }
    }

    // ========== FLASHLIGHT / TORCH ==========
    private var isFlashOn = false

    fun toggleFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashOn = turnOn
            if (turnOn) "Torch jala di! 🔦" else "Torch band kar di! 🔦"
        } catch (e: CameraAccessException) {
            "Torch mein problem aa gayi yaar!"
        }
    }

    // ========== VOLUME ==========
    fun controlVolume(action: String): String {
        val audioManager = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager
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
                "Phone mute kar diya! 🔇"
            }
            "max" -> {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI
                )
                "Volume full kar di! 🔊🔊🔊"
            }
            else -> "Volume kya karu? Badhaun ya kam karun?"
        }
    }

    // ========== BRIGHTNESS ==========
    fun controlBrightness(action: String): String {
        return try {
            // Need WRITE_SETTINGS permission
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return "Pehle brightness permission de yaar! Settings khol rahi hu."
            }

            // Auto brightness off karo
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
            "Brightness ${action} kar di! 💡 (${(newBrightness * 100) / 255}%)"
        } catch (e: Exception) {
            "Brightness change mein problem aa gayi!"
        }
    }

    // ========== MUSIC CONTROL ==========
    fun controlMedia(action: String): String {
        val audioManager = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        return when (action) {
            "play", "pause" -> {
                val keyEvent = if (action == "play")
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                else
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE

                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, keyEvent
                    )
                )
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP, keyEvent
                    )
                )
                if (action == "play") "Music play kar di! 🎵" 
                else "Music pause kar di! ⏸️"
            }
            "next" -> {
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                    )
                )
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                    )
                )
                "Agla gaana chala rahi hu! ⏭️"
            }
            else -> "Music kya karu? Play, pause ya next?"
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
            "Alarm set kar diya $hour:${String.format("%02d", minute)} pe! ⏰"
        } catch (e: Exception) {
            "Alarm set karne mein problem aa gayi!"
        }
    }

    // ========== BATTERY STATUS ==========
    fun getBatteryStatus(): String {
        val batteryManager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager
        
        val batteryLevel = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
        val isCharging = batteryManager.isCharging

        val emoji = when {
            batteryLevel > 80 -> "🔋"
            batteryLevel > 50 -> "🔋"
            batteryLevel > 20 -> "🪫"
            else -> "⚠️"
        }

        return "Battery $batteryLevel% hai $emoji" +
            if (isCharging) " aur charge ho rahi hai ⚡" else ""
    }

    // ========== TIME & DATE ==========
    fun getTimeDate(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("hi", "IN"))
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("hi", "IN"))
        
        val time = timeFormat.format(now)
        val date = dateFormat.format(now)
        
        return "Abhi $time baj rahe hain ⏰\nAaj $date hai 📅"
    }
}