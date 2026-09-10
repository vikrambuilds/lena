package com.vikram.lena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF7C4DFF),
    barCount: Int = 30
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp)
    ) {
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = (i * 2f + 1f) * barWidth
            val amplitude = size.height * 0.35f
            val barHeight = amplitude * (0.3f +
                    0.7f * kotlin.math.abs(
                sin(phase + i * 0.3f)
            ))

            drawLine(
                color = color.copy(
                    alpha = 0.4f + 0.6f * (barHeight / (amplitude * 1f))
                ),
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}