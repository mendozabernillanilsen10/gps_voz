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
import com.example.demoappchat.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecorderScreen(
    onAudioRecorded: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioPermission: PermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    // Timer para grabación
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
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

    if (audioPermission.status.isGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecording) "Grabando..." else "Presiona para grabar",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isRecording) {
                        Text(
                            text = formatAudioRecordingTime(recordingTime),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    FloatingActionButton(
                        onClick = {
                            if (isRecording) {
                                stopAudioRecording(mediaRecorder, audioFile) { uri ->
                                    if (uri != null) {
                                        onAudioRecorded(uri)
                                    }
                                }
                                isRecording = false
                                recordingTime = 0
                            } else {
                                val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                                audioFile = file
                                mediaRecorder = startAudioRecording(context, file.absolutePath) {
                                    isRecording = true
                                }
                            }
                        },
                        containerColor = if (isRecording) Error else PrimaryBlue,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Detener" else "Grabar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Gray600
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    } else {
        // Solicitar permisos
        LaunchedEffect(Unit) {
            audioPermission.launchPermissionRequest()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Permiso necesario",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gray900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Necesitamos acceso al micrófono para grabar audio",
                        fontSize = 14.sp,
                        color = Gray600,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { audioPermission.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Permitir")
                        }
                    }
                }
            }
        }
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

private fun formatAudioRecordingTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}