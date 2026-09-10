package com.vikram.lena.ui.screens

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    getSavedKeys: () -> Pair<String, String>,
    onSaveKeys: (String, String) -> Unit
) {
    val (savedGemini, savedOpenAI) = remember { getSavedKeys() }
    var geminiKey by remember { mutableStateOf(savedGemini) }
    var openaiKey by remember { mutableStateOf(savedOpenAI) }
    var showGemini by remember { mutableStateOf(false) }
    var showOpenAI by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "⚙️ Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // API Keys
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔑 API Keys",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Gemini
                    Text("Gemini API Key *", color = Color(0xFF00E5FF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste Gemini API key") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFF7C4DFF)
                        ),
                        visualTransformation = if (showGemini)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGemini = !showGemini }) {
                                Icon(
                                    if (showGemini) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null, tint = Color.White
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OpenAI
                    Text("OpenAI API Key (Optional)", color = Color(0xFF4CAF50), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = openaiKey,
                        onValueChange = { openaiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste OpenAI API key") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFF7C4DFF)
                        ),
                        visualTransformation = if (showOpenAI)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenAI = !showOpenAI }) {
                                Icon(
                                    if (showOpenAI) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null, tint = Color.White
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSaveKeys(geminiKey, openaiKey) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C4DFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save API Keys ✅", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Where to get keys
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📖 API Keys Kahan Se Lein?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    HelpItem(
                        "🔵 Gemini API Key (FREE)",
                        "1. aistudio.google.com/apikey pe jao\n" +
                                "2. Google se login karo\n" +
                                "3. 'Create API Key' pe click karo\n" +
                                "4. Key copy karo aur yahan paste karo"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpItem(
                        "🟢 OpenAI API Key (Paid, Optional)",
                        "1. platform.openai.com pe jao\n" +
                                "2. Sign up / Login karo\n" +
                                "3. API Keys section mein jao\n" +
                                "4. New key create karo"
                    )
                }
            }
        }

        // App Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ℹ️ About Lena", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version: 2.0", color = Color.White.copy(alpha = 0.7f))
                    Text("Developer: Vikram Kumar", color = Color.White.copy(alpha = 0.7f))
                    Text("B.Tech CSE, 5th Semester", color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Made with ❤️ using Gemini + ChatGPT",
                        color = Color(0xFF7C4DFF)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun HelpItem(title: String, description: String) {
    Text(title, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
}