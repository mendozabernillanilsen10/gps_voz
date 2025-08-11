package com.example.demoappchat.presentation.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.presentation.components.AudioRecorderScreen
import com.example.demoappchat.presentation.components.VideoPlayerScreen
import com.example.demoappchat.presentation.components.VideoRecorderScreen
import com.example.demoappchat.ui.theme.*
import com.example.demoappchat.presentation.recording.RecordingViewModel
import kotlinx.coroutines.delay
import android.media.MediaRecorder
import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import android.media.MediaPlayer

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
    val isGroupCallActive by viewModel.isGroupCallActive.collectAsState()
    val groupCallId by viewModel.groupCallId.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showMediaOptions by remember { mutableStateOf(false) }
    var showVideoRecorder by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf<Uri?>(null) }

    // Launcher para seleccionar imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendMediaMessage(chatId, it, "image")
        }
    }

    // Cargar chat al iniciar
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    // Scroll automático al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    

    


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat",
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Botón de videollamada grupal
                    IconButton(
                        onClick = { viewModel.startGroupCall(chatId, "VIDEO") },
                        enabled = !isGroupCallActive
                    ) {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = "Videollamada grupal",
                            tint = if (isGroupCallActive) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Botón de llamada de audio grupal
                    IconButton(
                        onClick = { viewModel.startGroupCall(chatId, "AUDIO") },
                        enabled = !isGroupCallActive
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Llamada de audio grupal",
                            tint = if (isGroupCallActive) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            // Banner de llamada grupal activa
            if (isGroupCallActive) {
                GroupCallBanner(
                    callId = groupCallId ?: "",
                    onEndCall = { viewModel.endGroupCall() }
                )
            }

            // Lista de mensajes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.userId == currentUser?.id
                    )
                }
            }

            // Panel de opciones de media
            if (showMediaOptions) {
                MediaOptionsPanel(
                    onImageSelected = {
                        imagePickerLauncher.launch("image/*")
                        showMediaOptions = false
                    },
                    onVideoRecord = {
                        showVideoRecorder = true
                        showMediaOptions = false
                    },
                    onDismiss = { showMediaOptions = false }
                )
            }

            // Barra de entrada de mensaje
            MessageInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSendMessage = {
                    if (messageText.trim().isNotEmpty()) {
                        viewModel.sendMessage(chatId, messageText.trim())
                        messageText = ""
                    }
                },
                onMediaClick = { showMediaOptions = true },
                isLoading = uiState.isLoading,
                recordingViewModel = recordingViewModel,
                onSendAudio = { audioUri ->
                    viewModel.sendMediaMessage(chatId, audioUri, "audio")
                }
            )
        }

        // Diálogos y pantallas
        if (showVideoRecorder) {
            VideoRecorderScreen(
                onVideoRecorded = { uri ->
                    viewModel.sendMediaMessage(chatId, uri, "video")
                    showVideoRecorder = false
                },
                onDismiss = { showVideoRecorder = false }
            )
        }

        if (showAudioRecorder) {
            AudioRecorderScreen(
                onAudioRecorded = { uri ->
                    viewModel.sendMediaMessage(chatId, uri, "audio")
                    showAudioRecorder = false
                },
                onDismiss = { showAudioRecorder = false }
            )
        }

        if (showVideoPlayer != null) {
            VideoPlayerScreen(
                videoUri = showVideoPlayer!!,
                onDismiss = { showVideoPlayer = null }
            )
        }

        // Snackbar para errores
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(uiState.error!!)
            }
        }
    }
}

@Composable
fun GroupCallBanner(
    callId: String,
    onEndCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                        Text(
            text = "Llamada grupal activa",
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Normal
        )
            }
            
            TextButton(
                onClick = onEndCall,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Terminar")
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean
) {
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isFromCurrentUser) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Nombre del usuario (solo para mensajes de otros)
        if (!isFromCurrentUser) {
            Text(
                text = message.userName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
        }

        // Burbuja del mensaje
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            )
        ) {
            when (message.messageType) {
                MessageType.TEXT -> {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(16.dp),
                        color = if (isFromCurrentUser) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                MessageType.PHOTO -> {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Imagen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                MessageType.VIDEO -> {
                    message.mediaUrl?.let { url ->
                        VideoPlayerBubble(
                            videoUrl = url,
                            isFromCurrentUser = isFromCurrentUser
                        )
                    }
                }
                MessageType.AUDIO -> {
                    message.mediaUrl?.let { url ->
                        AudioPlayerBubble(
                            audioUrl = url,
                            isFromCurrentUser = isFromCurrentUser
                        )
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(16.dp),
                        color = if (isFromCurrentUser) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Timestamp
        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AudioPlayerBubble(
    audioUrl: String,
    isFromCurrentUser: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }
    
    val textColor = if (isFromCurrentUser) 
        MaterialTheme.colorScheme.onPrimary 
    else 
        MaterialTheme.colorScheme.onSurface
    
    val iconColor = if (isFromCurrentUser) 
        MaterialTheme.colorScheme.onPrimary 
    else 
        MaterialTheme.colorScheme.primary
    
    // Inicializar MediaPlayer
    LaunchedEffect(audioUrl) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    duration = mp.duration
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerBubble", "Error inicializando MediaPlayer: ${e.message}")
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        }
    }
    
    // Actualizar posición
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(100)
                currentPosition = mediaPlayer?.currentPosition ?: 0
            }
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isFromCurrentUser) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón de reproducción
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Información del audio
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Mensaje de voz",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Barra de progreso
                LinearProgressIndicator(
                    progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = iconColor,
                    trackColor = textColor.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${formatAudioDuration(currentPosition)} / ${formatAudioDuration(duration)}",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerBubble(
    videoUrl: String,
    isFromCurrentUser: Boolean
) {
    var showVideoPlayer by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(12.dp)
            .clickable { showVideoPlayer = true },
        shape = RoundedCornerShape(12.dp),
        color = if (isFromCurrentUser) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Thumbnail del video (placeholder)
            AsyncImage(
                model = videoUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Overlay con botón de reproducción
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Reproducir video",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 4.dp)
                )
            }
            
            // Indicador de duración
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "Video",
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
    
    // Diálogo del reproductor de video
    if (showVideoPlayer) {
        VideoPlayerDialog(
            videoUrl = videoUrl,
            onDismiss = { showVideoPlayer = false }
        )
    }
}

@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        containerColor = Color.Black,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Aquí iría el reproductor de video real
                    // Por ahora mostramos un placeholder
                    AsyncImage(
                        model = videoUrl,
                        contentDescription = "Video",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Botón de cerrar
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMediaClick: () -> Unit,
    isLoading: Boolean,
    recordingViewModel: RecordingViewModel = hiltViewModel(),
    onSendAudio: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var showRecordingOptions by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val recordingState by recordingViewModel.recordingState.collectAsState()
    
    // Timer para la duración de grabación
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        } else {
            recordingDuration = 0
        }
    }
    
    // Cleanup MediaRecorder
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: RuntimeException) {
                    // Handle error
                }
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón de media (solo imagen y video)
        IconButton(
            onClick = onMediaClick,
            enabled = !isLoading && !isRecording
        ) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = "Adjuntar media",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Campo de texto
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Escribe un mensaje...") },
            enabled = !isLoading && !isRecording,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Botón de micrófono/enviar
        if (value.trim().isNotEmpty()) {
            // Botón de enviar cuando hay texto
            IconButton(
                onClick = onSendMessage,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Botón de micrófono cuando no hay texto
            IconButton(
                onClick = { 
                    if (isRecording) {
                        // Detener grabación
                        stopAudioRecording(mediaRecorder, audioFile) { uri ->
                            if (uri != null) {
                                onSendAudio(uri)
                            }
                        }
                        isRecording = false
                        showRecordingOptions = false
                        mediaRecorder = null
                        audioFile = null
                    } else {
                        // Iniciar grabación
                        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                        audioFile = file
                        mediaRecorder = startAudioRecording(context, file.absolutePath) {
                            isRecording = true
                            showRecordingOptions = true
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Grabar audio",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    // Opciones de grabación (aparecen cuando se presiona micrófono)
    if (showRecordingOptions && isRecording) {
        RecordingOptionsPanel(
            recordingDuration = recordingDuration,
            onSendRecording = {
                stopAudioRecording(mediaRecorder, audioFile) { uri ->
                    if (uri != null) {
                        onSendAudio(uri)
                    }
                }
                isRecording = false
                showRecordingOptions = false
                mediaRecorder = null
                audioFile = null
            },
            onDeleteRecording = {
                stopAudioRecording(mediaRecorder, audioFile) { }
                isRecording = false
                showRecordingOptions = false
                mediaRecorder = null
                audioFile = null
            },
            onCancelRecording = {
                stopAudioRecording(mediaRecorder, audioFile) { }
                isRecording = false
                showRecordingOptions = false
                mediaRecorder = null
                audioFile = null
            }
        )
    }
}

private fun startAudioRecording(
    context: Context,
    filePath: String,
    onStarted: () -> Unit
): MediaRecorder? {
    return try {
        MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(filePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
            start()
            onStarted()
        }
    } catch (e: IOException) {
        null
    }
}

private fun stopAudioRecording(
    mediaRecorder: MediaRecorder?,
    audioFile: File?,
    onStopped: (Uri?) -> Unit
) {
    try {
        mediaRecorder?.apply {
            stop()
            release()
        }

        audioFile?.let { file ->
            if (file.exists()) {
                onStopped(Uri.fromFile(file))
            } else {
                onStopped(null)
            }
        } ?: onStopped(null)
    } catch (e: RuntimeException) {
        onStopped(null)
    }
}

@Composable
fun RecordingOptionsPanel(
    recordingDuration: Int,
    onSendRecording: () -> Unit,
    onDeleteRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de grabación
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Grabando",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formatRecordingDuration(recordingDuration),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón cancelar
            IconButton(onClick = onCancelRecording) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancelar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Botón eliminar
            IconButton(onClick = onDeleteRecording) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            // Botón enviar
            IconButton(onClick = onSendRecording) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MediaOptionsPanel(
    onImageSelected: () -> Unit,
    onVideoRecord: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Seleccionar media",
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MediaOptionButton(
                    icon = Icons.Default.Image,
                    label = "Imagen",
                    onClick = onImageSelected
                )
                
                MediaOptionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    onClick = onVideoRecord
                )
            }
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
fun MediaOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val now = java.util.Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60000 -> "Ahora" // Menos de 1 minuto
        diff < 3600000 -> "${diff / 60000}m" // Menos de 1 hora
        diff < 86400000 -> "${diff / 3600000}h" // Menos de 1 día
        else -> java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(date)
    }
}

private fun formatRecordingDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, remainingSeconds)
    } else {
        String.format("%ds", remainingSeconds)
    }
}

private fun formatAudioDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}