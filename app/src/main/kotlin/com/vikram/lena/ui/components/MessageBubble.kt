package com.vikram.lena.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.data.Message

@Composable
fun MessageBubble(message: Message) {
    val isVikram = message.sender == "Vikram"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isVikram)
            Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isVikram) Color(0xFF7C4DFF)
                else Color(0xFF2A2A3E)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isVikram) 16.dp else 4.dp,
                bottomEnd = if (isVikram) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender
                Text(
                    text = if (isVikram) "Vikram 🙋‍♂️" else "Lena 🤖",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVikram) Color.White.copy(alpha = 0.7f)
                    else Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Message text
                Text(
                    text = message.message,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: time, model, task
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Timestamp
                    Text(
                        text = if (message.timestamp.length >= 19)
                            message.timestamp.substring(11, 19) // HH:MM:SS
                        else message.timestamp,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )

                    Row {
                        // Task type badge
                        if (message.taskType.isNotBlank() &&
                            message.taskType != "CONVERSATION"
                        ) {
                            Text(
                                text = message.taskType,
                                fontSize = 9.sp,
                                color = Color(0xFFFF9800).copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // AI model badge
                        if (message.aiModel.isNotBlank()) {
                            Text(
                                text = message.aiModel,
                                fontSize = 10.sp,
                                color = when (message.aiModel) {
                                    "Gemini" -> Color(0xFF2196F3)
                                    "ChatGPT" -> Color(0xFF4CAF50)
                                    "Local" -> Color(0xFFFF9800)
                                    else -> Color.Gray
                                }.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}