package com.vikram.lena.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.ui.components.GlassCard
import com.vikram.lena.ui.components.VoiceOrb
import com.vikram.lena.ui.theme.*

@Composable
fun HomeScreen(
    isListening: Boolean,
    isSpeaking: Boolean,
    volume: Float,
    statusText: String,
    hasApiKey: Boolean,
    isServiceRunning: Boolean,
    onOrbClick: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Hey Vikram 👋",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        "Lena AI v2.3",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceRunning) SuccessGreen.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.1f)
                        )
                        .clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isServiceRunning) SuccessGreen else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Wake Word Info Status Block
            AnimatedVisibility(
                visible = isServiceRunning && !isListening && !isSpeaking,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎧", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bas bolo: \"Lena\"",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Lena dynamic noise suppressor active",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            // API Key Warning
            AnimatedVisibility(
                visible = !hasApiKey,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "API Key nahi hai!",
                                color = WarningYellow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Only offline commands work",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = onNavigateToSettings) {
                            Text("Add", color = WarningYellow)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Re-designed Voice Orb (Barge-In reactive)
            VoiceOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                volume = volume,
                onClick = {
                    if (isServiceRunning) {
                        onOrbClick()
                    } else {
                        onStartService()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedContent(
                targetState = statusText,
                label = "status"
            ) { status ->
                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons block
            if (!isServiceRunning) {
                Button(
                    onClick = onStartService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "START LENA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onOrbClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Talk", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onStopService,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "✨ Try these commands",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val quickCommands = listOf(
                Triple("🔦", "Torch", "\"Torch jala do\""),
                Triple("📞", "Call", "\"Papa ko call karo\""),
                Triple("📱", "YouTube", "\"YouTube khol do\""),
                Triple("💬", "WhatsApp", "\"WhatsApp khol do\""),
                Triple("🔊", "Volume", "\"Volume badha do\""),
                Triple("🔋", "Battery", "\"Battery kitni hai?\""),
                Triple("⏰", "Time", "\"Kitne baje hain?\""),
                Triple("😂", "Joke", "\"Ek joke suna\""),
                Triple("💪", "Motivate", "\"Motivate kar mujhe\""),
                Triple("🎵", "Music", "\"Music chala do\"")
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickCommands) { (icon, title, example) ->
                    CommandCard(icon, title, example)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "What's new in v2.3?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Sweet Voice & Echo Cancellation active.\n" +
                        "• Speak in middle of response to stop Lena (Barge-In).\n" +
                        "• Long responses chunked to prevent early cut-offs.\n" +
                        "• System and notification sound beeps muted completely.\n" +
                        "• Latest Gemini 2.0 Flash integration.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CommandCard(icon: String, title: String, example: String) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                example,
                color = TextTertiary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}