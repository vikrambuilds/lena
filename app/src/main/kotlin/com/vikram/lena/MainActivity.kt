package com.vikram.lena

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vikram.lena.core.LenaService
import com.vikram.lena.data.ConversationManager
import com.vikram.lena.ui.screens.ChatScreen
import com.vikram.lena.ui.screens.HomeScreen
import com.vikram.lena.ui.screens.SettingsScreen
import com.vikram.lena.ui.theme.LenaTheme

class MainActivity : ComponentActivity() {

    private lateinit var conversationManager: ConversationManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLenaService()
            requestBatteryOptimization()
        } else {
            Toast.makeText(this, "Sari permissions de do please! 🙏", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationManager = ConversationManager(this)

        setContent {
            LenaTheme {
                LenaMainApp(
                    conversationManager = conversationManager,
                    onStart = { requestPermissionsAndStart() },
                    onStop = { stopLenaService() },
                    onExport = { exportCSV() },
                    onShare = { shareCSV() },
                    onDelete = { deleteCSV() },
                    onSaveApiKeys = { gemini, openai -> saveApiKeys(gemini, openai) },
                    getSavedKeys = { getSavedApiKeys() }
                )
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startLenaService() {
        val intent = Intent(this, LenaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Lena activated! 🎉🤖", Toast.LENGTH_SHORT).show()
    }

    private fun stopLenaService() {
        stopService(Intent(this, LenaService::class.java))
        Toast.makeText(this, "Lena deactivated 😴", Toast.LENGTH_SHORT).show()
    }

    private fun requestBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ))
        }
    }

    private fun exportCSV() {
        val (success, path) = conversationManager.exportToDownloads()
        Toast.makeText(
            this,
            if (success) "CSV exported to Downloads/Lena/ 📥" else "Export failed: $path",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareCSV() {
        conversationManager.shareCSV()
    }

    private fun deleteCSV() {
        conversationManager.deleteAllConversations()
        Toast.makeText(this, "All conversations deleted! 🗑️", Toast.LENGTH_SHORT).show()
    }

    private fun saveApiKeys(gemini: String, openai: String) {
        getSharedPreferences("lena_prefs", Context.MODE_PRIVATE).edit()
            .putString("gemini_key", gemini)
            .putString("openai_key", openai)
            .apply()

        LenaService.GEMINI_API_KEY = gemini
        LenaService.OPENAI_API_KEY = openai
        Toast.makeText(this, "API Keys saved! ✅", Toast.LENGTH_SHORT).show()
    }

    private fun getSavedApiKeys(): Pair<String, String> {
        val prefs = getSharedPreferences("lena_prefs", Context.MODE_PRIVATE)
        return Pair(
            prefs.getString("gemini_key", "") ?: "",
            prefs.getString("openai_key", "") ?: ""
        )
    }
}

@Composable
fun LenaMainApp(
    conversationManager: ConversationManager,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSaveApiKeys: (String, String) -> Unit,
    getSavedKeys: () -> Pair<String, String>
) {
    var currentScreen by remember { mutableStateOf("home") }
    var isServiceRunning by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(conversationManager.getAllMessages()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            messages = conversationManager.getAllMessages()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, "Chat") },
                    label = { Text("Chat") },
                    selected = currentScreen == "chat",
                    onClick = { currentScreen = "chat" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == "settings",
                    onClick = { currentScreen = "settings" }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0F0F23))
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    isRunning = isServiceRunning,
                    messageCount = messages.size,
                    fileSize = conversationManager.getFileSize(),
                    dailySummary = conversationManager.getDailySummary(),
                    onToggle = {
                        if (isServiceRunning) onStop() else onStart()
                        isServiceRunning = !isServiceRunning
                    },
                    onExport = onExport,
                    onShare = onShare,
                    onDelete = onDelete
                )
                "chat" -> ChatScreen(messages = messages)
                "settings" -> SettingsScreen(
                    getSavedKeys = getSavedKeys,
                    onSaveKeys = onSaveApiKeys
                )
            }
        }
    }
}