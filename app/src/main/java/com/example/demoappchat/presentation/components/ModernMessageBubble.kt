package com.example.demoappchat.presentation.components

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    onImageClick: (Uri) -> Unit = {},
    onVideoClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Burbuja del mensaje con diseño minimalista
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = if (isOwnMessage) 2.dp else 1.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isOwnMessage) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                    else 
                        Color.Black.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            border = if (!isOwnMessage) {
                BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            } else null
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Contenido del mensaje según el tipo
                when (message.messageType) {
                    MessageType.PHOTO -> {
                        PhotoMessageContent(
                            mediaUrl = message.mediaUrl ?: "",
                            content = message.content,
                            isOwnMessage = isOwnMessage,
                            onImageClick = onImageClick
                        )
                    }
                    MessageType.AUDIO -> {
                        AudioMessageContent(
                            mediaUrl = message.mediaUrl ?: "",
                            content = message.content,
                            isOwnMessage = isOwnMessage,
                            duration = "0:45"
                        )
                    }
                    MessageType.VIDEO -> {
                        VideoMessageContent(
                            mediaUrl = message.mediaUrl ?: "",
                            content = message.content,
                            isOwnMessage = isOwnMessage,
                            onVideoClick = onVideoClick
                        )
                    }
                    else -> {
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }

    // Timestamp y estado del mensaje
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timestamp
            Text(
                text = formatTime(message.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )

            // Indicador de estado para mensajes propios
            if (isOwnMessage) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = "Enviado",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (isOwnMessage) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun PhotoMessageContent(
    mediaUrl: String,
    content: String,
    isOwnMessage: Boolean,
    onImageClick: (Uri) -> Unit = {}
) {
    var showFullScreenImage by remember { mutableStateOf(false) }
    var isImageLoading by remember { mutableStateOf(true) }
    var imageLoadError by remember { mutableStateOf(false) }

    Column {
        // Contenedor de la imagen con esquinas redondeadas y diseño minimalista
        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isOwnMessage) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { 
                    showFullScreenImage = true
                    try {
                        onImageClick(Uri.parse(mediaUrl))
                    } catch (e: Exception) {
                        // Fallback para mostrar en pantalla completa
                    }
                }
        ) {
            // Imagen principal con mejor manejo de errores
            if (mediaUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto enviada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = {
                        isImageLoading = true
                        imageLoadError = false
                    },
                    onSuccess = {
                        isImageLoading = false
                        imageLoadError = false
                    },
                    onError = {
                        isImageLoading = false
                        imageLoadError = true
                    }
                )
            } else {
                // Estado cuando no hay URL
                imageLoadError = true
                isImageLoading = false
            }

            // Loading indicator mejorado
            if (isImageLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isOwnMessage) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cargando...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error state mejorado para modo oscuro/claro
            if (imageLoadError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isOwnMessage) 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.BrokenImage,
                            contentDescription = "Error al cargar imagen",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Error al cargar",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (mediaUrl.isNotEmpty()) "URL: ${mediaUrl.take(30)}..." else "Sin URL",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Overlay con gradiente sutil en la parte inferior para mejor legibilidad
            if (!isImageLoading && !imageLoadError && mediaUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
        }

        // Texto descriptivo si existe
        if (content.isNotEmpty() && content != mediaUrl) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = if (isOwnMessage) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    // Full screen image viewer
    if (showFullScreenImage) {
        FullScreenImageViewer(
            imageUrl = mediaUrl,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // Botón cerrar
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    CircleShape
                )
                .size(44.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Imagen con zoom y pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)

                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2

                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen en pantalla completa",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Indicadores de zoom en la parte inferior
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Pellizca para hacer zoom",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}



// Funciones auxiliares
private fun formatTime(timestamp: Long): String {
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



private fun startRecording(filePath: String, onStarted: () -> Unit): MediaRecorder? {
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

private fun stopRecording(mediaRecorder: MediaRecorder?, onStopped: (Uri) -> Unit) {
    try {
        mediaRecorder?.apply {
            stop()
            release()
        }
    } catch (e: RuntimeException) {
        // Handle error
    }
}



@Composable
fun VideoMessageContent(
    mediaUrl: String,
    content: String,
    isOwnMessage: Boolean,
    onVideoClick: (Uri) -> Unit = {}
) {
    var isVideoLoading by remember { mutableStateOf(true) }
    var videoLoadError by remember { mutableStateOf(false) }

    Column {
        // Contenedor del video con preview y diseño minimalista
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isOwnMessage) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    else 
                        Color.Black
                )
                .clickable { 
                    try {
                        if (mediaUrl.isNotEmpty()) {
                            onVideoClick(Uri.parse(mediaUrl))
                        } else {
                            videoLoadError = true
                        }
                    } catch (e: Exception) {
                        videoLoadError = true
                    }
                }
        ) {
            // Thumbnail del video o loading
            if (videoLoadError) {
                // Error state mejorado
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = if (isOwnMessage) 
                            MaterialTheme.colorScheme.error 
                        else 
                            Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error al cargar video",
                        color = if (isOwnMessage) 
                            MaterialTheme.colorScheme.error 
                        else 
                            Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (mediaUrl.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sin URL de video",
                            color = if (isOwnMessage) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (isVideoLoading) {
                // Loading state mejorado
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = if (isOwnMessage) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cargando video...",
                            color = if (isOwnMessage) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Video thumbnail (placeholder mejorado)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Fondo con gradiente sutil
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        if (isOwnMessage) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else 
                                            Color.Black.copy(alpha = 0.8f),
                                        if (isOwnMessage) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        else 
                                            Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    // Icono de play centrado
                    Surface(
                        color = if (isOwnMessage) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Reproducir video",
                                tint = if (isOwnMessage) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Indicador de duración (placeholder)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isOwnMessage) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                else 
                    Color.Black.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "0:15", // Placeholder duration
                    color = if (isOwnMessage) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Texto descriptivo si existe
        if (content.isNotEmpty() && content != mediaUrl) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = if (isOwnMessage) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}