package com.example.demoappchat.presentation.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.presentation.components.AudioRecorderScreen
import com.example.demoappchat.presentation.components.MediaOptionsPanel
import com.example.demoappchat.presentation.components.VideoPlayerScreen
import com.example.demoappchat.presentation.components.VideoRecorderScreen
import com.example.demoappchat.ui.theme.EmergencyRed
import com.example.demoappchat.ui.theme.Gray700
import com.example.demoappchat.ui.theme.PrimaryBlue
import com.example.demoappchat.ui.theme.SafetyGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.demoappchat.presentation.components.*
import com.example.demoappchat.presentation.recording.RecordingViewModel
import com.example.demoappchat.ui.theme.Gray600
import com.example.demoappchat.ui.theme.Gray900
import com.example.demoappchat.ui.theme.MessageBackground
import com.example.demoappchat.presentation.chat.VideoCallActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    recordingViewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val recordingState by recordingViewModel.recordingState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val isUploading = uiState.isLoading
    var pendingMediaToUpload by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var showMediaOptions by remember { mutableStateOf(false) }
    var showVideoRecorder by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf<Uri?>(null) }

    // Estados para paginación
    var isLoadingMoreMessages by remember { mutableStateOf(false) }
    var canLoadMore by remember { mutableStateOf(true) }

    // Funciones para lanzar captura
    fun startVideoRecording() {
        showVideoRecorder = true
        showMediaOptions = false
    }

    fun startPhotoCapture() {
        val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
        val photoUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", photoFile)
        pendingMediaToUpload = photoUri to "photo"
        showMediaOptions = false
    }

    fun startAudioRecording() {
        showAudioRecorder = true
        showMediaOptions = false
    }

    // Upload effect
    LaunchedEffect(pendingMediaToUpload) {
        val (uri, type) = pendingMediaToUpload ?: return@LaunchedEffect
        viewModel.uploadAndSendMedia(chatId, uri, type, context)
        pendingMediaToUpload = null
    }

    // Load data
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
        viewModel.loadMessages(chatId)
    }

    // Auto scroll
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Detectar scroll hacia arriba para cargar más mensajes
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    messages.isNotEmpty()
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && canLoadMore && !isLoadingMoreMessages) {
                isLoadingMoreMessages = true
                // Aquí llamarías a viewModel.loadMoreMessages()
                // Por ahora simulo la carga
                kotlinx.coroutines.delay(1000)
                isLoadingMoreMessages = false
            }
        }
    }

    // Set current chat ID for voice service
    LaunchedEffect(chatId) {
        viewModel.setCurrentChatId(chatId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentChatId()
        }
    }

    // Helper function for video calls
    fun startVideoCall(context: android.content.Context, chatId: String, participantName: String) {
        val intent = Intent(context, VideoCallActivity::class.java).apply {
            putExtra(VideoCallActivity.EXTRA_CHAT_ID, chatId)
            putExtra(VideoCallActivity.EXTRA_PARTICIPANT_NAME, participantName)
            putExtra(VideoCallActivity.EXTRA_IS_INCOMING_CALL, false)
        }
        context.startActivity(intent)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ModernChatTopBar(
                    chatTitle = uiState.currentChat?.title ?: "Chat",
                    participantCount = uiState.currentChat?.participantsCount ?: 0,
                    chatId = chatId,
                    onNavigateBack = onNavigateBack,
                    onVideoCall = {
                        // Iniciar videollamada grupal con notificaciones FCM
                        viewModel.startGroupVideoCall(chatId)
                        // También abrir la actividad de videollamada
                        startVideoCall(context, chatId, uiState.currentChat?.title ?: "Chat")
                    },
                    onVoiceCall = {
                        // Iniciar llamada de audio grupal con notificaciones FCM
                        viewModel.startGroupAudioCall(chatId)
                        // También abrir la actividad de videollamada (solo audio)
                        startVideoCall(context, chatId, uiState.currentChat?.title ?: "Chat")
                    }
                )
            },
            bottomBar = {
                ModernMessageInputBar(
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(chatId, messageText, MessageType.TEXT)
                            messageText = ""
                        }
                    },
                    onAttachmentClick = { showMediaOptions = !showMediaOptions },
                    isLoading = isUploading
                )
            }
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MessageBackground)
            ) {
                if (messages.isEmpty() && !uiState.isLoading) {
                    ModernEmptyChatContent(
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = false
                    ) {
                        // Indicador de carga al inicio (mensajes más antiguos)
                        if (isLoadingMoreMessages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            ModernMessageBubble(
                                message = message,
                                isOwnMessage = message.userId == currentUser?.id,
                                onVideoClick = { uri ->
                                    showVideoPlayer = uri
                                },
                                onImageClick = { uri ->
                                    // Implementar visualizador de imágenes
                                }
                            )
                        }
                    }
                }

                // Indicador de grabación mejorado (mejor posicionado)
                AnimatedVisibility(
                    visible = recordingState.isRecording,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = paddingValues.calculateTopPadding() + 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    RecordingIndicator(
                        isRecording = recordingState.isRecording,
                        recordingTime = recordingState.recordingTime,
                        recordingType = recordingState.recordingType
                    )
                }

                // Indicador de micrófono mejorado (mejor posicionado y más discreto)
                AnimatedVisibility(
                    visible = recordingState.isListening && !recordingState.isRecording,
                    enter = scaleIn(
                        initialScale = 0.3f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0.3f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = paddingValues.calculateTopPadding() + 16.dp,
                            end = 16.dp
                        )
                ) {
                    FloatingMicIndicator(
                        isListening = recordingState.isListening
                    )
                }
            }
        }

        // Media options overlay
        AnimatedVisibility(
            visible = showMediaOptions,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showMediaOptions = false }
            )

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MediaOptionsPanel(
                    onPhotoClick = { startPhotoCapture() },
                    onVideoClick = { startVideoRecording() },
                    onAudioClick = { startAudioRecording() },
                    onDismiss = { showMediaOptions = false }
                )
            }
        }

        // Loading overlay mejorado
        AnimatedVisibility(
            visible = isUploading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Subiendo archivo...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray700
                        )
                    }
                }
            }
        }

        // Pantallas de grabación
        if (showVideoRecorder) {
            VideoRecorderScreen(
                onVideoRecorded = { uri ->
                    showVideoRecorder = false
                    viewModel.uploadAndSendMedia(chatId, uri, "video", context)
                },
                onDismiss = { showVideoRecorder = false }
            )
        }

        if (showAudioRecorder) {
            AudioRecorderScreen(
                onAudioRecorded = { uri ->
                    showAudioRecorder = false
                    viewModel.uploadAndSendMedia(chatId, uri, "audio", context)
                },
                onDismiss = { showAudioRecorder = false }
            )
        }

        showVideoPlayer?.let { uri ->
            VideoPlayerScreen(
                videoUri = uri,
                onDismiss = { showVideoPlayer = null }
            )
        }
    }
}

// Componente mejorado para el indicador de micrófono
@Composable
fun FloatingMicIndicator(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    if (isListening) {
        Card(
            modifier = modifier
                .size(56.dp)
                .scale(scale),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = SafetyGreen.copy(alpha = alpha)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Escuchando",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
    }
}

// Componente mejorado para el indicador de grabación
@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    recordingTime: Long,
    recordingType: String,
    modifier: Modifier = Modifier
) {
    if (isRecording) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = EmergencyRed
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Punto pulsante
                val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "recording_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color.White.copy(alpha = alpha),
                            CircleShape
                        )
                )

                Text(
                    text = when (recordingType) {
                        "audio" -> "Grabando audio"
                        "video" -> "Grabando video"
                        else -> "Grabando"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatRecordingTime(recordingTime),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

private fun formatRecordingTime(timeInMillis: Long): String {
    val seconds = timeInMillis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}


@Composable
fun ModernEmptyChatContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono principal
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    PrimaryBlue.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = PrimaryBlue.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Comienza la conversación!",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Gray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Envía el primer mensaje para conectar con tu grupo",
            fontSize = 15.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}