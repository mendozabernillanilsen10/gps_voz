package com.example.demoappchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
            .background(MaterialTheme.colorScheme.background)
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
        ModernChatInput(
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
    isVoiceServiceActive: Boolean = false,
    onToggleVoiceService: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column {
            // Barra principal del header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Título del chat
                Text(
                    text = "Chat",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Botón de servicio de voz
                IconButton(
                    onClick = { onToggleVoiceService(!isVoiceServiceActive) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isVoiceServiceActive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isVoiceServiceActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isVoiceServiceActive) "Desactivar voz" else "Activar voz",
                        tint = if (isVoiceServiceActive) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Indicador de estado del servicio de voz
            if (isVoiceServiceActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Servicio de voz activo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
    val backgroundColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Burbuja del mensaje
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isFromCurrentUser) 20.dp else 4.dp,
                topEnd = if (isFromCurrentUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = backgroundColor,
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Reproducir video",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendImage: () -> Unit,
    onSendVideo: () -> Unit,
    onSendAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón de media con diseño minimalista
            IconButton(
                onClick = onSendImage,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Adjuntar media",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Campo de texto minimalista
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Escribe un mensaje...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            )

            // Botón de enviar con diseño minimalista
            IconButton(
                onClick = onSendMessage,
                enabled = value.trim().isNotEmpty(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (value.trim().isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (value.trim().isNotEmpty()) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp

    return when {
        diff < 60 * 1000 -> "Ahora"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
} 