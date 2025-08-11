package com.example.demoappchat.presentation.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerFullScreen(
    videoUrl: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    val videoUri = runCatching { Uri.parse(videoUrl) }.getOrNull()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri ?: Uri.EMPTY))
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            totalDuration = duration
                            isLoading = false
                        }
                        Player.STATE_BUFFERING -> {
                            isLoading = true
                        }
                        Player.STATE_ENDED -> {
                            isPlaying = false
                            currentPosition = 0
                        }
                    }
                }
                
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
            prepare()
        }
    }

    // Actualizar posición del video
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(100)
        }
    }

    // Los controles siempre visibles
    LaunchedEffect(Unit) {
        showControls = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { 
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Solo botón de regreso minimalista
        if (!isLoading) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(24.dp)
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun formatVideoTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
