package com.example.demoappchat.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.example.demoappchat.ui.theme.SecurityChatTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class VideoCallActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_PARTICIPANT_NAME = "participant_name"
        const val EXTRA_IS_INCOMING_CALL = "is_incoming_call"
        
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    private var chatId: String? = null
    private var participantName: String? = null
    private var isIncomingCall: Boolean = false
    
    private lateinit var cameraExecutor: ExecutorService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME)
        isIncomingCall = intent.getBooleanExtra(EXTRA_IS_INCOMING_CALL, false)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (!allPermissionsGranted()) {
            requestPermissions()
        }
        
        setContent {
            SecurityChatTheme {
                VideoCallScreen(
                    chatId = chatId ?: "",
                    participantName = participantName ?: "Desconocido",
                    isIncomingCall = isIncomingCall,
                    onEndCall = { finish() },
                    onToggleCamera = { /* TODO: Implement camera toggle */ },
                    onToggleMicrophone = { /* TODO: Implement mic toggle */ },
                    onToggleSpeaker = { /* TODO: Implement speaker toggle */ }
                )
            }
        }
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!allPermissionsGranted()) {
                finish() // Close if permissions denied
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun VideoCallScreen(
    chatId: String,
    participantName: String,
    isIncomingCall: Boolean,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    var isCallConnected by remember { mutableStateOf(!isIncomingCall) }
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isMicrophoneEnabled by remember { mutableStateOf(true) }
    var isSpeakerEnabled by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }
    
    // Timer for call duration
    LaunchedEffect(isCallConnected) {
        if (isCallConnected) {
            while (isCallConnected) {
                kotlinx.coroutines.delay(1000)
                callDuration++
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video preview (placeholder for actual video implementation)
        VideoPreview(
            modifier = Modifier.fillMaxSize(),
            isCameraEnabled = isCameraEnabled
        )
        
        // Incoming call overlay
        if (isIncomingCall && !isCallConnected) {
            IncomingCallOverlay(
                participantName = participantName,
                onAccept = { isCallConnected = true },
                onDecline = onEndCall,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Connected call UI
        if (isCallConnected) {
            ConnectedCallUI(
                participantName = participantName,
                callDuration = callDuration,
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
                isSpeakerEnabled = isSpeakerEnabled,
                onEndCall = onEndCall,
                onToggleCamera = {
                    isCameraEnabled = !isCameraEnabled
                    onToggleCamera()
                },
                onToggleMicrophone = {
                    isMicrophoneEnabled = !isMicrophoneEnabled
                    onToggleMicrophone()
                },
                onToggleSpeaker = {
                    isSpeakerEnabled = !isSpeakerEnabled
                    onToggleSpeaker()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Top status bar
        CallStatusBar(
            participantName = participantName,
            callDuration = if (isCallConnected) callDuration else 0,
            isConnected = isCallConnected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )
    }
}

@Composable
fun VideoPreview(
    modifier: Modifier = Modifier,
    isCameraEnabled: Boolean
) {
    if (isCameraEnabled) {
        CameraPreview(modifier = modifier)
    } else {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = "Cámara desactivada",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cámara desactivada",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun IncomingCallOverlay(
    participantName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Llamada entrante",
                tint = Color(0xFF1877F2),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Llamada entrante",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Text(
                text = participantName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Decline button
                Surface(
                    onClick = onDecline,
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Rechazar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Accept button
                Surface(
                    onClick = onAccept,
                    shape = CircleShape,
                    color = Color(0xFF42C85F),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Aceptar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedCallUI(
    participantName: String,
    callDuration: Int,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker toggle
            Surface(
                onClick = onToggleSpeaker,
                shape = CircleShape,
                color = if (isSpeakerEnabled) Color(0xFF1877F2) else Color.Gray,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSpeakerEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Altavoz",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Microphone toggle
            Surface(
                onClick = onToggleMicrophone,
                shape = CircleShape,
                color = if (isMicrophoneEnabled) Color(0xFF1877F2) else Color.Red,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Micrófono",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // End call
            Surface(
                onClick = onEndCall,
                shape = CircleShape,
                color = Color.Red,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Colgar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Camera toggle
            Surface(
                onClick = onToggleCamera,
                shape = CircleShape,
                color = if (isCameraEnabled) Color(0xFF1877F2) else Color.Red,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Cámara",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Switch camera (front/back)
            Surface(
                onClick = { /* TODO: Implement camera switch */ },
                shape = CircleShape,
                color = Color.Gray,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraFront,
                        contentDescription = "Cambiar cámara",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallStatusBar(
    participantName: String,
    callDuration: Int,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = participantName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (isConnected) {
                Text(
                    text = formatCallDuration(callDuration),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "Conectando...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // Handle camera binding errors
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun formatCallDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}