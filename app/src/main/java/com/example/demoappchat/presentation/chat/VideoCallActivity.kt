package com.example.demoappchat.presentation.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.demoappchat.R
import com.example.demoappchat.ui.theme.*

class VideoCallActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_PARTICIPANT_NAME = "participant_name"
        const val EXTRA_IS_INCOMING_CALL = "is_incoming_call"
    }
    
    private var chatId: String = ""
    private var participantName: String = ""
    private var isIncomingCall: Boolean = false
    private var callId: String = ""
    private var callType: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener extras del intent
        chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: ""
        participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME) ?: ""
        isIncomingCall = intent.getBooleanExtra(EXTRA_IS_INCOMING_CALL, false)
        callId = intent.getStringExtra("call_id") ?: ""
        callType = intent.getStringExtra("call_type") ?: "video"
        
        Log.d("VideoCallActivity", "📞 Iniciando actividad de videollamada")
        Log.d("VideoCallActivity", "👤 Participante: $participantName")
        Log.d("VideoCallActivity", "📹 Tipo: $callType")
        Log.d("VideoCallActivity", "🆔 Call ID: $callId")
        
        setContent {
            SecurityChatTheme {
                VideoCallScreen(
                    participantName = participantName,
                    callType = callType,
                    isIncomingCall = isIncomingCall,
                    onEndCall = { endCall() },
                    onToggleCamera = { toggleCamera() },
                    onToggleMicrophone = { toggleMicrophone() }
                )
            }
        }
        
        // Solicitar permisos si es necesario
        requestPermissions()
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("VideoCallActivity", "✅ Permisos concedidos")
        } else {
            Log.w("VideoCallActivity", "⚠️ Algunos permisos fueron denegados")
        }
    }
    
    private fun endCall() {
        Log.d("VideoCallActivity", "📞 Finalizando llamada")
        // TODO: Implementar lógica para finalizar la llamada WebRTC
        finish()
    }
    
    private fun toggleCamera() {
        Log.d("VideoCallActivity", "📹 Alternando cámara")
        // TODO: Implementar lógica para activar/desactivar cámara
    }
    
    private fun toggleMicrophone() {
        Log.d("VideoCallActivity", "🎤 Alternando micrófono")
        // TODO: Implementar lógica para activar/desactivar micrófono
    }
}

@Composable
fun VideoCallScreen(
    participantName: String,
    callType: String,
    isIncomingCall: Boolean,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit
) {
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isMicrophoneEnabled by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MinimalistBlueMuted.copy(alpha = 0.1f),
                        BackgroundPrimary
                    )
                )
            )
    ) {
        // Fondo principal con gradiente sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MinimalistBlueMuted.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 800f
                    )
                )
        )
        
        // Contenido principal centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Avatar/Icono del participante
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = MinimalistBlue.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MinimalistBlue.copy(alpha = 0.1f),
                                MinimalistBlueMuted.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (callType == "video") Icons.Outlined.Videocam else Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MinimalistBlue
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Nombre del participante
            Text(
                text = participantName,
                color = adaptiveTextPrimary(),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tipo de llamada
            Text(
                text = if (callType == "video") "Videollamada" else "Llamada de audio",
                color = adaptiveTextSecondary(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estado de conexión
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MinimalistGreen,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conectando...",
                    color = adaptiveTextSecondary(),
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Controles de llamada
            CallControls(
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
                callType = callType,
                onToggleCamera = {
                    isCameraEnabled = !isCameraEnabled
                    onToggleCamera()
                },
                onToggleMicrophone = {
                    isMicrophoneEnabled = !isMicrophoneEnabled
                    onToggleMicrophone()
                },
                onEndCall = onEndCall
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CallControls(
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    callType: String,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón de micrófono
        CallControlButton(
            icon = if (isMicrophoneEnabled) Icons.Outlined.Mic else Icons.Outlined.MicOff,
            isActive = isMicrophoneEnabled,
            onClick = onToggleMicrophone,
            backgroundColor = if (isMicrophoneEnabled) MinimalistGreen else MinimalistGreenMuted,
            iconColor = if (isMicrophoneEnabled) Color.White else MinimalistGreen
        )
        
        // Botón de cámara (solo para videollamadas)
        if (callType == "video") {
            CallControlButton(
                icon = if (isCameraEnabled) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                isActive = isCameraEnabled,
                onClick = onToggleCamera,
                backgroundColor = if (isCameraEnabled) MinimalistBlue else MinimalistBlueMuted,
                iconColor = if (isCameraEnabled) Color.White else MinimalistBlue
            )
        }
        
        // Botón de colgar
        CallControlButton(
            icon = Icons.Outlined.CallEnd,
            isActive = false,
            onClick = onEndCall,
            backgroundColor = Error,
            iconColor = Color.White,
            size = 72.dp
        )
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconColor: Color,
    size: Dp = 56.dp
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (isActive) 8.dp else 4.dp,
                shape = CircleShape,
                spotColor = backgroundColor.copy(alpha = 0.3f)
            ),
        containerColor = backgroundColor,
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(if (size.value > 56.dp.value) 32.dp else 24.dp)
        )
    }
}