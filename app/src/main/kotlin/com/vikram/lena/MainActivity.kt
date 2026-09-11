package com.vikram.lena

import android.Manifest
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
import androidx.compose.runtime.*
import com.vikram.lena.core.LenaService
import com.vikram.lena.data.PreferencesManager
import com.vikram.lena.ui.screens.HomeScreen
import com.vikram.lena.ui.screens.OnboardingScreen
import com.vikram.lena.ui.screens.SettingsScreen
import com.vikram.lena.ui.theme.LenaTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            startLenaService()
            requestBatteryOptimization()
        } else {
            Toast.makeText(this, "Mic permission chahiye yaar!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        
        setContent {
            LenaTheme {
                LenaApp()
            }
        }
    }
    
    @Composable
    fun LenaApp() {
        var currentScreen by remember { 
            mutableStateOf(
                if (preferencesManager.isOnboardingComplete) "home" else "onboarding"
            ) 
        }
        
        var isServiceRunning by remember { mutableStateOf(LenaService.isRunning) }
        var isListening by remember { mutableStateOf(false) }
        var isSpeaking by remember { mutableStateOf(false) }
        var volume by remember { mutableStateOf(0f) }
        var statusText by remember { mutableStateOf("Tap orb to talk 🤖") }
        var hasApiKey by remember { mutableStateOf(preferencesManager.hasApiKey()) }
        
        // Register service callbacks
        DisposableEffect(Unit) {
            LenaService.onStatusChanged = { status ->
                statusText = status
                isListening = status.contains("🎤") || status.contains("Sun rahi")
                isSpeaking = status.contains("🗣️") || status.contains("bol rahi")
            }
            LenaService.onVolumeChanged = { vol ->
                volume = vol
            }
            
            onDispose {
                LenaService.onStatusChanged = null
                LenaService.onVolumeChanged = null
            }
        }
        
        // Check service status periodically
        LaunchedEffect(Unit) {
            while (true) {
                isServiceRunning = LenaService.isRunning
                hasApiKey = preferencesManager.hasApiKey()
                kotlinx.coroutines.delay(1000)
            }
        }
        
        when (currentScreen) {
            "onboarding" -> OnboardingScreen(
                onComplete = { gemini, openai ->
                    preferencesManager.geminiApiKey = gemini
                    preferencesManager.openaiApiKey = openai
                    preferencesManager.isOnboardingComplete = true
                    hasApiKey = preferencesManager.hasApiKey()
                    currentScreen = "home"
                    Toast.makeText(this@MainActivity, "Setup complete! 🎉", Toast.LENGTH_SHORT).show()
                },
                onSkip = {
                    preferencesManager.isOnboardingComplete = true
                    currentScreen = "home"
                    Toast.makeText(this@MainActivity, "You can add API keys later in Settings", Toast.LENGTH_LONG).show()
                }
            )
            
            "home" -> HomeScreen(
                isListening = isListening,
                isSpeaking = isSpeaking,
                volume = volume,
                statusText = statusText,
                hasApiKey = hasApiKey,
                isServiceRunning = isServiceRunning,
                onOrbClick = {
                    if (isServiceRunning) {
                        triggerListening()
                    } else {
                        requestPermissionsAndStart()
                    }
                },
                onStartService = { requestPermissionsAndStart() },
                onStopService = { stopLenaService() },
                onNavigateToSettings = { currentScreen = "settings" }
            )
            
            "settings" -> SettingsScreen(
                initialGeminiKey = preferencesManager.geminiApiKey,
                initialOpenaiKey = preferencesManager.openaiApiKey,
                onSaveKeys = { gemini, openai ->
                    preferencesManager.geminiApiKey = gemini
                    preferencesManager.openaiApiKey = openai
                    hasApiKey = preferencesManager.hasApiKey()
                    Toast.makeText(this@MainActivity, "Keys saved! ✅", Toast.LENGTH_SHORT).show()
                },
                onBack = { currentScreen = "home" }
            )
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
        Toast.makeText(this, "Lena activated! 🚀", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopLenaService() {
        stopService(Intent(this, LenaService::class.java))
        Toast.makeText(this, "Lena stopped 😴", Toast.LENGTH_SHORT).show()
    }
    
    private fun triggerListening() {
        val intent = Intent(this, LenaService::class.java).apply {
            action = LenaService.ACTION_START_LISTENING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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
}