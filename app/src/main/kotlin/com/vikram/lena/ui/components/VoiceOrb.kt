package com.vikram.lena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.lena.ui.theme.ElectricBlue
import com.vikram.lena.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    volume: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    // Pulse animation
    val pulseScale by animateFloatAsState(
        targetValue = when {
            isListening -> 1.15f + volume * 0.3f
            isSpeaking -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pulse"
    )
    
    // Color animation
    val glowIntensity by animateFloatAsState(
        targetValue = if (isListening || isSpeaking) 1f else 0.5f,
        label = "glow"
    )
    
    Box(
        modifier = modifier
            .size(240.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow rings
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            
            // Multiple rotating rings
            for (i in 0..2) {
                val ringRotation = rotation + (i * 120f)
                val ringRadius = maxRadius * (0.7f + i * 0.1f)
                val alpha = (0.3f - i * 0.1f) * glowIntensity
                
                // Ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = alpha),
                            ElectricBlue.copy(alpha = alpha),
                            NeonPurple.copy(alpha = alpha)
                        ),
                        center = center
                    ),
                    radius = ringRadius,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Rotating dots on ring
                for (j in 0..5) {
                    val dotAngle = (ringRotation + j * 60f) * (Math.PI.toFloat() / 180f)
                    val dotX = center.x + ringRadius * cos(dotAngle)
                    val dotY = center.y + ringRadius * sin(dotAngle)
                    
                    drawCircle(
                        color = if (i == 0) NeonPurple else ElectricBlue,
                        radius = (4.dp.toPx() + volume * 8.dp.toPx()),
                        center = Offset(dotX, dotY),
                        alpha = alpha * 2f
                    )
                }
            }
            
            // Inner glowing orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.9f),
                        ElectricBlue.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.5f
                ),
                radius = maxRadius * 0.5f,
                center = center
            )
            
            // Core white glow
            drawCircle(
                color = Color.White.copy(alpha = 0.3f * glowIntensity),
                radius = maxRadius * 0.25f,
                center = center
            )
        }
        
        // Center icon/text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isSpeaking -> "🗣️"
                    isListening -> "🎤"
                    else -> "🤖"
                },
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isSpeaking -> "Speaking..."
                    isListening -> "Listening..."
                    else -> "Tap to talk"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}