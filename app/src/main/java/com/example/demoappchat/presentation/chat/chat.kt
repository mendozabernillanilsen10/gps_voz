package com.example.demoappchat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.ui.theme.EmergencyRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Cargar chat y mensajes
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
        viewModel.loadMessages(chatId)
    }

    // Hacer scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.currentChat?.title ?: "Chat",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${uiState.currentChat?.participantsCount ?: 0} participantes",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyRed
                ),
                actions = {
                    IconButton(onClick = { /* Mostrar info del chat */ }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Información",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(chatId, messageText, MessageType.TEXT)
                        messageText = ""
                    }
                },
                onSendPhoto = { /* Implementar foto */ },
                onSendAudio = { /* Implementar audio */ }
            )
        }
    ) { paddingValues ->

        if (messages.isEmpty()) {
            EmptyChatContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isOwnMessage = message.userId == currentUser?.id
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // Avatar del usuario
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(EmergencyRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.userName.firstOrNull()?.toString() ?: "U",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Nombre del usuario (solo si no es propio)
            if (!isOwnMessage) {
                Text(
                    text = message.userName,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            // Burbuja del mensaje
            Card(
                shape = RoundedCornerShape(
                    topStart = if (isOwnMessage) 16.dp else 4.dp,
                    topEnd = if (isOwnMessage) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwnMessage) EmergencyRed else Color.Gray.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    when (message.messageType) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                color = if (isOwnMessage) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }

                        MessageType.PHOTO -> {
                            // Placeholder para foto
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isOwnMessage) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Foto",
                                    color = if (isOwnMessage) Color.White else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        MessageType.AUDIO -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isOwnMessage) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Audio",
                                    color = if (isOwnMessage) Color.White else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        else -> {
                            Text(
                                text = message.content,
                                color = if (isOwnMessage) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Hora del mensaje
            Text(
                text = formatTime(message.timestamp),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(
                    start = if (isOwnMessage) 0.dp else 8.dp,
                    end = if (isOwnMessage) 8.dp else 0.dp,
                    top = 2.dp
                )
            )
        }

        if (isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            // Avatar propio (opcional)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendPhoto: () -> Unit,
    onSendAudio: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Botón de foto
            IconButton(
                onClick = onSendPhoto,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Enviar foto",
                    tint = EmergencyRed
                )
            }

            // Campo de texto
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de enviar/audio
            if (messageText.isBlank()) {
                IconButton(
                    onClick = onSendAudio,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Grabar audio",
                        tint = EmergencyRed
                    )
                }
            } else {
                IconButton(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmergencyRed, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar mensaje",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Inicia la conversación",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Text(
            text = "Sé el primero en enviar un mensaje",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}