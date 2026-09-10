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
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.vikram.lena.core.LenaService
import com.vikram.lena.data.ConversationManager
import com.vikram.lena.data.Message

class MainActivity : ComponentActivity() {

    private lateinit var conversationManager: ConversationManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLenaService()
            requestBatteryOptimization()
        } else {
            Toast.makeText(this, "Sari permissions de do please! 🙏", 
                Toast.LENGTH_LONG).show()
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
            Manifest.permission.CAMERA,
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
        Toast.makeText(this,
            if (success) "CSV exported to: Downloads/Lena/ 📥" 
            else "Export failed: $path",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareCSV() { conversationManager.shareCSV() }

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

// ==================== THEME ====================

@Composable
fun LenaTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF7C4DFF),
        onPrimary = Color.White,
        surface = Color(0xFF1A1A2E),
        onSurface = Color.White,
        background = Color(0xFF0F0F23),
        onBackground = Color.White,
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFFF6B6B)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// ==================== MAIN APP COMPOSABLE ====================

@OptIn(ExperimentalMaterial3Api::class)
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

    // Auto refresh
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
                    onToggle = {
                        if (isServiceRunning) onStop() else onStart()
                        isServiceRunning = !isServiceRunning
                    },
                    onExport = onExport,
                    onShare = onShare,
                    onDelete = onDelete,
                    dailySummary = conversationManager.getDailySummary()
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

// ==================== HOME SCREEN ====================

@Composable
fun HomeScreen(
    isRunning: Boolean,
    messageCount: Int,
    fileSize: String,
    onToggle: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    dailySummary: String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) Color(0xFF1B5E20) 
                                     else Color(0xFF2A2A3E)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) "🟢 LENA IS ACTIVE" 
                               else "⚪ LENA IS OFFLINE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRunning) "\"Lena\" bolke baat karo!" 
                               else "Start karo mujhe!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFF44336) 
                                             else Color(0xFF7C4DFF)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isRunning) "STOP LENA" else "START LENA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "💬", value = "$messageCount", label = "Messages"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "📁", value = fileSize, label = "CSV Size"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🤖", value = "v2.0", label = "Version"
                )
            }
        }

        // Daily Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Today's Summary", fontWeight = FontWeight.Bold,
                        color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dailySummary, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // Quick Actions
        item {
            Text("⚡ Quick Actions", fontWeight = FontWeight.Bold,
                color = Color.White, fontSize = 18.sp)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(Modifier.weight(1f), "📥", "Export") { onExport() }
                ActionButton(Modifier.weight(1f), "📤", "Share") { onShare() }
                ActionButton(Modifier.weight(1f), "🗑️", "Delete") { 
                    showDeleteDialog = true 
                }
            }
        }

        // Capabilities
        item {
            Text("🎯 What Lena Can Do", fontWeight = FontWeight.Bold,
                color = Color.White, fontSize = 18.sp)
        }

        item {
            val capabilities = listOf(
                "🗣️ Voice Chat" to "\"Lena\" bolke baat karo",
                "📞 Phone Calls" to "\"Mummy ko call karo\"",
                "📱 Open Apps" to "\"YouTube khol do\"",
                "💬 WhatsApp" to "\"Rahul ko WhatsApp karo\"",
                "📶 WiFi/BT" to "\"WiFi on karo\"",
                "🔦 Flashlight" to "\"Torch jala do\"",
                "🔊 Volume" to "\"Volume badha do\"",
                "⏰ Alarm" to "\"7 baje alarm lagao\"",
                "🔋 Battery" to "\"Battery kitni hai?\"",
                "✉️ SMS" to "\"Rahul ko SMS bhejo\"",
                "🎵 Music" to "\"Gaana chala do\""
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                capabilities.forEach { (title, example) ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A2A3E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, fontSize = 14.sp, color = Color.White,
                                modifier = Modifier.weight(1f))
                            Text(example, fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("🗑️ Delete All?", color = Color.White) },
            text = { Text("Sari conversations permanently delete ho jayengi!",
                color = Color.White.copy(alpha = 0.7f)) },
            containerColor = Color(0xFF2A2A3E),
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("DELETE") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: String, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 24.sp)
            Text(value, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ActionButton(modifier: Modifier, icon: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 14.sp)
        }
    }
}

// ==================== CHAT SCREEN ====================

@Composable
fun ChatScreen(messages: List<Message>) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💬 Chat History", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Text("${messages.size} messages", fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f))
        }

        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Abhi tak koi conversation nahi hui!",
                        color = Color.White.copy(alpha = 0.5f))
                    Text("\"Lena\" bolke baat shuru karo!",
                        color = Color(0xFF7C4DFF))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isVikram = message.sender == "Vikram"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isVikram) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isVikram) Color(0xFF7C4DFF) 
                                 else Color(0xFF2A2A3E)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isVikram) 16.dp else 4.dp,
                bottomEnd = if (isVikram) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender name
                Text(
                    text = if (isVikram) "Vikram 🙋‍♂️" else "Lena 🤖",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVikram) Color.White.copy(alpha = 0.7f)
                           else Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Message
                Text(message.message, color = Color.White, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.timestamp.takeLast(8), // HH:MM:SS
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    if (message.aiModel.isNotEmpty()) {
                        Text(
                            text = message.aiModel,
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF).copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== SETTINGS SCREEN ====================

@Composable
fun SettingsScreen(
    getSavedKeys: () -> Pair<String, String>,
    onSaveKeys: (String, String) -> Unit
) {
    val (savedGemini, savedOpenAI) = getSavedKeys()
    var geminiKey by remember { mutableStateOf(savedGemini) }
    var openaiKey by remember { mutableStateOf(savedOpenAI) }
    var showGemini by remember { mutableStateOf(false) }
    var showOpenAI by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("⚙️ Settings", fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
        }

        // API Keys Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔑 API Keys", fontWeight = FontWeight.Bold,
                        color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Gemini Key
                    Text("Gemini API Key", color = Color.White, fontSize = 14.sp)
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste Gemini API key here") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C4DFF),
                        ),
                        visualTransformation = if (showGemini)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGemini = !showGemini }) {
                                Icon(
                                    if (showGemini) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OpenAI Key
                    Text("OpenAI API Key (Optional)", color = Color.White, fontSize = 14.sp)
                    OutlinedTextField(
                        value = openaiKey,
                        onValueChange = { openaiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste OpenAI API key here") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C4DFF),
                        ),
                        visualTransformation = if (showOpenAI)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenAI = !showOpenAI }) {
                                Icon(
                                    if (showOpenAI) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSaveKeys(geminiKey, openaiKey) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C4DFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save API Keys ✅")
                    }
                }
            }
        }

        // API Key Help
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📖 API Keys Kahan Se Milenge?",
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("🔵 Gemini (FREE):", color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold)
                    Text("→ aistudio.google.com/apikey",
                        color = Color.White.copy(alpha = 0.7f))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("🟢 OpenAI (Optional, Paid):", color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold)
                    Text("→ platform.openai.com/api-keys",
                        color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // Commands Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 Voice Commands Guide",
                        fontWeight = FontWeight.Bold, color = Color.White,
                        fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val commands = listOf(
                        "📞 Calls" to "\"Lena, Mummy ko call karo\"\n\"Call utha lo\" / \"Call reject karo\"",
                        "📱 Apps" to "\"Lena, YouTube khol do\"\n\"WhatsApp open karo\"",
                        "💬 Messages" to "\"Rahul ko SMS bhejo ki late ho jaunga\"\n\"WhatsApp pe Priya ko message karo\"",
                        "📶 System" to "\"WiFi on karo\" / \"Bluetooth off karo\"\n\"Torch jala do\"",
                        "🔊 Audio" to "\"Volume badha do\" / \"Mute karo\"\n\"Gaana chala do\" / \"Next song\"",
                        "⏰ Alarm" to "\"7 baje alarm laga do\"\n\"Yaad dila dena assignment ki\"",
                        "ℹ️ Info" to "\"Battery kitni hai?\"\n\"Kya time hua hai?\"",
                        "🗣️ Chat" to "\"Lena, kaisi hai?\"\n\"Yaar, DSA samjha do\""
                    )

                    commands.forEach { (category, examples) ->
                        Text(category, fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C4DFF))
                        Text(examples, color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}