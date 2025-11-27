package com.example.chatapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TypingIndicator() {
    val dotCount = 3
    val dotSize = 8.dp
    val dotSpacing = 4.dp
    val animationDelay = 200L

    val animations = List(dotCount) {
        remember { Animatable(0f) }
    }

    animations.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * animationDelay)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        0.0f at 0
                        1.0f at 400
                        0.0f at 800
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    val dy = with(LocalDensity.current) { dotSize.toPx() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        animations.forEach { animatable ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        translationY = -animatable.value * dy
                    }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(dotSpacing))
        }
    }
}