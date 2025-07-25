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
import com.example.demoappchat.ui.theme.Gray600
import com.example.demoappchat.ui.theme.Gray900
import com.example.demoappchat.ui.theme.MessageBackground

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
    val context = LocalContext.current

    var isUploading by remember { mutableStateOf(false) }
    var pendingMediaToUpload by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var showMediaOptions by remember { mutableStateOf(false) }
    var showVideoRecorder by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf<Uri?>(null) }

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
        isUploading = true
        viewModel.uploadAndSendMedia(chatId, uri, type, context)
        isUploading = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {

                ModernChatTopBar(
                    chatTitle = uiState.currentChat?.title ?: "Chat",
                    participantCount = uiState.currentChat?.participantsCount ?: 0,
                    onNavigateBack = onNavigateBack
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
                if (messages.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            ModernMessageBubble(
                                message = message,
                                isOwnMessage = message.userId == currentUser?.id
                            )
                        }
                    }
                }
            }
        }

        // Media options overlay
        if (showMediaOptions) {
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

        // Loading overlay
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    isUploading = true
                    viewModel.uploadAndSendMedia(chatId, uri, "video", context)
                    isUploading = false
                },
                onDismiss = { showVideoRecorder = false }
            )
        }

        if (showAudioRecorder) {
            AudioRecorderScreen(
                onAudioRecorded = { uri ->
                    showAudioRecorder = false
                    isUploading = true
                    viewModel.uploadAndSendMedia(chatId, uri, "audio", context)
                    isUploading = false
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