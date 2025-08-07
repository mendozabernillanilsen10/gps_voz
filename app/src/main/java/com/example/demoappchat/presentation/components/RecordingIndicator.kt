package com.example.demoappchat.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * INDICADOR VISUAL DE GRABACIÓN POLICIAL
 * Muestra cuando se está grabando audio/video automáticamente
 */
@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    recordingTime: Int = 0,
    recordingType: String = "AUDIO",
    modifier: Modifier = Modifier
) {
    if (isRecording) {
        // Animación de pulsación
        val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        Card(
            modifier = modifier
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ícono animado
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Grabando",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    // Texto principal
                    Text(
                        text = "🔴 GRABANDO",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Información adicional
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = recordingType.lowercase(),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = formatTime(recordingTime),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * INDICADOR DE MICRÓFONO FLOTANTE
 * Para mostrar en la esquina de la pantalla
 */
@Composable
fun FloatingMicIndicator(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    if (isListening) {
        val infiniteTransition = rememberInfiniteTransition(label = "mic_listening")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_scale"
        )

        Card(
            modifier = modifier
                .size(56.dp)
                .scale(scale),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Escuchando comandos",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * INDICADOR COMPACTO PARA BARRA SUPERIOR
 */
@Composable
fun CompactRecordingIndicator(
    isRecording: Boolean,
    recordingTime: Int = 0,
    modifier: Modifier = Modifier
) {
    if (isRecording) {
        val infiniteTransition = rememberInfiniteTransition(label = "compact_recording")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "compact_alpha"
        )

        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.error.copy(alpha = alpha),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Grabando",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = formatTime(recordingTime),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}