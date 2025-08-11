package com.example.demoappchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VideoCallComposable(
    chatId: String,
    isVideoCall: Boolean = true,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    isMuted: Boolean = false,
    isCameraOn: Boolean = true,
    isSpeakerOn: Boolean = false,
    participantCount: Int = 1
) {
    var showControls by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video principal (placeholder por ahora)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isVideoCall) Icons.Default.Videocam else Icons.Default.Mic,
                        contentDescription = if (isVideoCall) "Video" else "Audio",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = if (isVideoCall) "Videollamada grupal" else "Llamada de audio grupal",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 220.dp)
            )
            
            Text(
                text = "$participantCount participantes",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Controles superiores
        if (showControls) {
            TopControls(
                onEndCall = onEndCall,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onToggleSpeaker = onToggleSpeaker,
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isSpeakerOn = isSpeakerOn,
                isVideoCall = isVideoCall
            )
        }
        
        // Controles inferiores
        if (showControls) {
            BottomControls(
                onEndCall = onEndCall,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onToggleSpeaker = onToggleSpeaker,
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isSpeakerOn = isSpeakerOn,
                isVideoCall = isVideoCall
            )
        }
        
        // Botón para mostrar/ocultar controles
        IconButton(
            onClick = { showControls = !showControls },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Mostrar/ocultar controles",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TopControls(
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    isVideoCall: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón de altavoz
        IconButton(
            onClick = onToggleSpeaker,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isSpeakerOn) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                    CircleShape
                )
        ) {
            Icon(
                if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = "Altavoz",
                tint = if (isSpeakerOn) Color.White else Color.White
            )
        }
        
        // Botón de cámara (solo para videollamadas)
        if (isVideoCall) {
            IconButton(
                onClick = onToggleCamera,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isCameraOn) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Cámara",
                    tint = if (isCameraOn) Color.White else Color.White
                )
            }
        }
        
        // Botón de micrófono
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (!isMuted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                    CircleShape
                )
        ) {
            Icon(
                if (!isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Micrófono",
                tint = if (!isMuted) Color.White else Color.White
            )
        }
    }
}

@Composable
private fun BottomControls(
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    isVideoCall: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón de terminar llamada
        IconButton(
            onClick = onEndCall,
            modifier = Modifier
                .size(64.dp)
                .background(
                    Color.Red,
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "Terminar llamada",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun VideoCallStatusBar(
    isActive: Boolean,
    callType: String,
    participantCount: Int,
    onJoinCall: () -> Unit,
    onEndCall: () -> Unit
) {
    if (isActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Column {
                        Text(
                            text = if (callType == "VIDEO") "Videollamada grupal" else "Llamada de audio grupal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$participantCount participantes",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onJoinCall,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Unirse")
                    }
                    
                    TextButton(
                        onClick = onEndCall,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Terminar")
                    }
                }
            }
        }
    }
}