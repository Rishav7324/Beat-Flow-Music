package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Song
import com.example.utils.AudioTrimmerUtil
import kotlinx.coroutines.launch

@Composable
fun AudioTrimmerScreen(currentSong: Song?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pcmData by remember { mutableStateOf<List<Float>>(emptyList()) }
    var startPercentage by remember { mutableStateOf(0f) }
    var endPercentage by remember { mutableStateOf(1f) }
    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            isExtracting = true
            pcmData = AudioTrimmerUtil.extractPcmData(context, currentSong.uri)
            startPercentage = 0f
            endPercentage = 1f
            isExtracting = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentSong == null) {
            Text("Select a song to trim", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Text(
            text = "AUDIO TRIMMER",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(currentSong.title, style = MaterialTheme.typography.titleLarge)
        Text(currentSong.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        if (isExtracting) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Extracting wave data via MediaCodec...", style = MaterialTheme.typography.bodySmall)
        } else if (pcmData.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                val waveColor = MaterialTheme.colorScheme.primary
                val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)

                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp)) {
                    val width = size.width
                    val height = size.height
                    val barWidth = 4f
                    val gap = 4f
                    val totalBars = (width / (barWidth + gap)).toInt()
                    
                    val step = (pcmData.size / totalBars.coerceAtLeast(1))
                    
                    for (i in 0 until totalBars) {
                        val index = (i * step).coerceIn(0, pcmData.size - 1)
                        val amp = pcmData[index] * height
                        val x = i * (barWidth + gap)

                        val isSelected = (x / width) in startPercentage..endPercentage
                        val color = if (isSelected) waveColor else inactiveColor
                        
                        drawLine(
                            color = color,
                            start = Offset(x, height / 2f - amp / 2f),
                            end = Offset(x, height / 2f + amp / 2f),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                    // Trimmer Controls
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val maxW = maxWidth.value
                        // Left handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(16.dp)
                                .offset(x = (startPercentage * maxW).dp)
                                .background(MaterialTheme.colorScheme.error)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        startPercentage = (startPercentage + dragAmount / (maxW * 2.5f)).coerceIn(0f, endPercentage - 0.05f)
                                    }
                                }
                        )
                        // Right handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(16.dp)
                                .offset(x = (endPercentage * maxW - 16).dp)
                                .background(MaterialTheme.colorScheme.error)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        endPercentage = (endPercentage + dragAmount / (maxW * 2.5f)).coerceIn(startPercentage + 0.05f, 1f)
                                    }
                                }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        val startMillis = (startPercentage * currentSong.duration).toLong()
                        val endMillis = (endPercentage * currentSong.duration).toLong()
                        AudioTrimmerUtil.saveTrimmedAudio(context, currentSong.path, startMillis, endMillis, "${currentSong.title}_ringtone")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE AS RINGTONE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
