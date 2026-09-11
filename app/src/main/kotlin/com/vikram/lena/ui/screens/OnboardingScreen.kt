package com.vikram.lena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.ui.components.GlassCard
import com.vikram.lena.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: (geminiKey: String, openaiKey: String) -> Unit,
    onSkip: () -> Unit
) {
    var geminiKey by remember { mutableStateOf("") }
    var openaiKey by remember { mutableStateOf("") }
    var showGemini by remember { mutableStateOf(false) }
    var showOpenai by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo
            Text("🤖", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Welcome to Lena!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                "Your AI Best Friend",
                fontSize = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Setup Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "🔑 Setup API Keys",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Text(
                        "Lena ko baat karne ke liye AI key chahiye. Gemini FREE hai!",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    
                    // Gemini Key
                    Text(
                        "🔵 Gemini API Key (FREE - Recommended)",
                        fontSize = 14.sp,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("AIzaSy...", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = NeonPurple
                        ),
                        visualTransformation = if (showGemini) VisualTransformation.None 
                                              else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGemini = !showGemini }) {
                                Icon(
                                    if (showGemini) Icons.Default.VisibilityOff 
                                    else Icons.Default.Visibility,
                                    null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // OpenAI Key
                    Text(
                        "🟢 OpenAI Key (Optional, Paid)",
                        fontSize = 14.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = openaiKey,
                        onValueChange = { openaiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-proj-...", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = NeonPurple
                        ),
                        visualTransformation = if (showOpenai) VisualTransformation.None 
                                              else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showOpenai = !showOpenai }) {
                                Icon(
                                    if (showOpenai) Icons.Default.VisibilityOff 
                                    else Icons.Default.Visibility,
                                    null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Help Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "💡 Gemini Key Kaise Le?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "1. Chrome mein jao: aistudio.google.com/apikey\n" +
                        "2. Google se login karo\n" +
                        "3. 'Create API Key' click karo\n" +
                        "4. Key copy karke yahan paste karo!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Button(
                onClick = { onComplete(geminiKey, openaiKey) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = geminiKey.isNotBlank() || openaiKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Save & Continue 🚀",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onSkip) {
                Text(
                    "Skip for now (Only offline features)",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}