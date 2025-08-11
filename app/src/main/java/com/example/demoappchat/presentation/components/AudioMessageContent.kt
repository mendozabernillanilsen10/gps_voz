package com.example.demoappchat.presentation.components
import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.demoappchat.ui.theme.Error
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

@Composable
fun AudioMessageContent(
    mediaUrl: String,
    content: String,
    isOwnMessage: Boolean,
    duration: String
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var currentPosition by remember { mutableStateOf(0f) }
    var totalDuration by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    
    // Validar que la URL no sea nula o vacía
    val isValidUrl = mediaUrl.isNotBlank() && mediaUrl != "null"

    // Cleanup MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (!isValidUrl) {
                    hasError = true
                    return@IconButton
                }
                
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            try {
                                setDataSource(mediaUrl)
                                prepareAsync()
                                setOnPreparedListener { mp ->
                                    totalDuration = mp.duration
                                    mp.start()
                                    isPlaying = true
                                    hasError = false
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0f
                                }
                                setOnErrorListener { mp, what, extra ->
                                    hasError = true
                                    isPlaying = false
                                    true
                                }
                            } catch (e: Exception) {
                                hasError = true
                                android.util.Log.e("AudioMessageContent", "Error setting up MediaPlayer", e)
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isOwnMessage) Color.White.copy(alpha = 0.25f) else Error.copy(alpha = 0.15f),
                    CircleShape
                )
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = if (hasError) Color.Gray else if (isOwnMessage) Color.White else Error,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Progress bar
            LinearProgressIndicator(
                progress = if (totalDuration > 0) currentPosition / totalDuration else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = if (isOwnMessage) Color.White else Error,
                trackColor = if (isOwnMessage) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = duration,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOwnMessage) Color.White.copy(alpha = 0.9f) else Color.Gray
            )
        }

        Icon(
            Icons.Rounded.Mic,
            contentDescription = null,
            tint = if (isOwnMessage) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }

    // Update progress
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            currentPosition = mediaPlayer!!.currentPosition.toFloat()
            delay(100)
        }
    }
}