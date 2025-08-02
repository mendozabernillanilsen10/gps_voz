package com.example.demoappchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Componente de chat moderno y elegante al estilo Instagram/Facebook
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatUI(
    messages: List<ChatMessage>,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onSendAudio: () -> Unit,
    isVoiceServiceActive: Boolean = false,
    onToggleVoiceService: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
    ) {
        // Header elegante - FIXED
        ModernChatHeader(
            isVoiceServiceActive = isVoiceServiceActive,
            onToggleVoiceService = onToggleVoiceService
        )
        
        // Lista de mensajes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(
                items = messages,
                key = { message -> 
                    // Use a combination of fields to ensure uniqueness
                    // If id is empty, use timestamp + userId as fallback
                    if (message.id.isNotBlank()) {
                        message.id
                    } else {
                        "${message.timestamp}_${message.userId}"
                    }
                }
            ) { message ->
                ModernMessageBubble(
                    message = message,
                    isFromCurrentUser = message.userId == currentUserId,
                    modifier = Modifier
                )
            }
        }
        
        // Input moderno
        ModernMessageInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSendMessage = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                }
            },
            onSendImage = onSendImage,
            onSendVideo = onSendVideo,
            onSendAudio = onSendAudio
        )
    }
}

@Composable
fun ModernChatHeader(
    isVoiceServiceActive: Boolean,
    onToggleVoiceService: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Chat Grupal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )
                    Text(
                        text = if (isVoiceServiceActive) "🎤 Escuchando comandos" else "💬 Chat activo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )
                }
            }
            
            // Toggle de servicio de voz
            Switch(
                checked = isVoiceServiceActive,
                onCheckedChange = onToggleVoiceService,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryBlue,
                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = Gray600,
                    uncheckedTrackColor = Gray600.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun ModernMessageBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isFromCurrentUser) PrimaryBlue else Color.White
    val textColor = if (isFromCurrentUser) Color.White else Gray900
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            )
        ) {
            when (message.messageType) {
                MessageType.TEXT -> {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
                MessageType.PHOTO -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Imagen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                MessageType.VIDEO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Gray600)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Reproducir video",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                MessageType.AUDIO -> {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Reproducir audio",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }
        
        // Información del mensaje
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isFromCurrentUser) {
                Text(
                    text = message.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Gray600
            )
        }
    }
}

@Composable
fun ModernMessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onSendAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Barra de herramientas de medios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de imagen
                IconButton(
                    onClick = onSendImage,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Gray600.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Enviar imagen",
                        tint = Gray600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Botón de video
                IconButton(
                    onClick = onSendVideo,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Gray600.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Enviar video",
                        tint = Gray600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Botón de audio
                IconButton(
                    onClick = onSendAudio,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Gray600.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Enviar audio",
                        tint = Gray600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Input de texto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Escribe un mensaje...",
                            color = Gray600
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Gray600.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                
                // Botón de enviar
                IconButton(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (value.isNotBlank()) PrimaryBlue else Gray600.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    enabled = value.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60000 -> "Ahora"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
} 