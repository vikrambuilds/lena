package com.vikram.lena.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.data.ConversationManager

@Composable
fun ConversationScreen(conversationManager: ConversationManager) {
    val messages = remember { mutableStateOf(conversationManager.getAllMessages()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            messages.value = conversationManager.getAllMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "📊 Conversation Analytics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        val allMessages = messages.value
        val vikramCount = allMessages.count { it.sender == "Vikram" }
        val lenaCount = allMessages.count { it.sender == "Lena" }
        val geminiCount = allMessages.count { it.aiModel == "Gemini" }
        val gptCount = allMessages.count { it.aiModel == "ChatGPT" }
        val taskMessages = allMessages.filter { it.taskType.isNotBlank() }

        // Stats Cards
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCard(Modifier.weight(1f), "Total", "${allMessages.size}", Color(0xFF7C4DFF))
                    AnalyticsCard(Modifier.weight(1f), "Vikram", "$vikramCount", Color(0xFF4CAF50))
                    AnalyticsCard(Modifier.weight(1f), "Lena", "$lenaCount", Color(0xFF00E5FF))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCard(Modifier.weight(1f), "Gemini", "$geminiCount", Color(0xFF2196F3))
                    AnalyticsCard(Modifier.weight(1f), "ChatGPT", "$gptCount", Color(0xFF4CAF50))
                    AnalyticsCard(Modifier.weight(1f), "Tasks", "${taskMessages.size}", Color(0xFFFF9800))
                }
            }

            // Task breakdown
            if (taskMessages.isNotEmpty()) {
                item {
                    Text(
                        "🎯 Task Breakdown",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                val taskCounts = taskMessages
                    .groupBy { it.taskType }
                    .map { (type, msgs) -> type to msgs.size }
                    .sortedByDescending { it.second }

                items(taskCounts) { (taskType, count) ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A2A3E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(taskType, color = Color.White)
                            Text(
                                "$count times",
                                color = Color(0xFF7C4DFF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AnalyticsCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 24.sp)
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}