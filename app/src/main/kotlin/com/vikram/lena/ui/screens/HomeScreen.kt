package com.vikram.lena.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.ui.components.StatusCard
import com.vikram.lena.ui.components.WaveAnimation

@Composable
fun HomeScreen(
    isRunning: Boolean,
    messageCount: Int,
    fileSize: String,
    memoryCount: Int = 0,
    dailySummary: String,
    onToggle: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Status Card
        item {
            StatusCard(
                isRunning = isRunning,
                onToggle = onToggle
            )
        }

        // Wave Animation when running
        if (isRunning) {
            item {
                WaveAnimation()
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMiniCard(Modifier.weight(1f), "💬", "$messageCount", "Messages")
                StatMiniCard(Modifier.weight(1f), "📁", fileSize, "CSV Size")
                StatMiniCard(Modifier.weight(1f), "🧠", "$memoryCount", "Memories")
                StatMiniCard(Modifier.weight(1f), "🤖", "v2.0", "Version")
            }
        }

        // Daily Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 Today's Summary",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dailySummary, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // Quick Actions
        item {
            Text(
                "⚡ Quick Actions",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(Modifier.weight(1f), "📥", "Export CSV") { onExport() }
                QuickActionCard(Modifier.weight(1f), "📤", "Share CSV") { onShare() }
                QuickActionCard(Modifier.weight(1f), "🗑️", "Delete All") {
                    showDeleteDialog = true
                }
            }
        }

        // Voice Commands Guide
        item {
            Text(
                "🎯 Voice Commands",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        val commandGroups = listOf(
            Triple("📞", "Calls", "\"Mummy ko call karo\" | \"Call utha lo\""),
            Triple("📱", "Apps", "\"YouTube khol do\" | \"WhatsApp open karo\""),
            Triple("💬", "Messages", "\"Rahul ko SMS bhejo\" | \"WhatsApp message karo\""),
            Triple("📶", "System", "\"WiFi on karo\" | \"Bluetooth off karo\""),
            Triple("🔊", "Audio", "\"Volume badha do\" | \"Gaana chala do\""),
            Triple("⏰", "Alarm", "\"7 baje alarm lagao\" | \"Yaad dila dena\""),
            Triple("ℹ️", "Info", "\"Battery kitni hai?\" | \"Time kya hua?\""),
            Triple("🔦", "Torch", "\"Torch jala do\" | \"Light band karo\""),
            Triple("🗣️", "Chat", "\"Lena kaisi hai?\" | \"DSA samjha do\""),
        )

        commandGroups.forEach { (icon, title, example) ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                example, fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("🗑️ Sab Delete Karein?", color = Color.White) },
            text = {
                Text(
                    "Sari conversations permanently delete ho jayengi!\nYe action undo nahi hoga.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            containerColor = Color(0xFF2A2A3E),
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("DELETE ALL") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun StatMiniCard(modifier: Modifier, icon: String, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun QuickActionCard(modifier: Modifier, icon: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}