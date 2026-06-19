package com.example.ui

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun VinylRecord(isPlaying: Boolean, playbackSpeed: Float, uri: Uri?, modifier: Modifier = Modifier) {
    var currentRotation by remember { mutableStateOf(0f) }
    val rotation = remember { androidx.compose.animation.core.Animatable(currentRotation) }

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = currentRotation + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween((3000 / playbackSpeed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                currentRotation = value
            }
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.Black)
            .border(4.dp, Color.DarkGray, CircleShape)
            .graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f - (i * 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.35f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                coil.compose.AsyncImage(
                    model = uri,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Black))
        }
    }
}
