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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.demoappchat.R

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
            VideoCallScreen(
                participantName = participantName,
                callType = callType,
                isIncomingCall = isIncomingCall,
                onEndCall = { endCall() },
                onToggleCamera = { toggleCamera() },
                onToggleMicrophone = { toggleMicrophone() }
            )
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
            .background(Color(0xFF1A1A1A))
    ) {
        // Fondo de video (placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C2C2C))
        ) {
            // Placeholder para el video del participante
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(
                        color = Color(0xFF3C3C3C),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = participantName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (callType == "video") "Videollamada" else "Llamada de audio",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        // Controles de llamada (parte inferior)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Controles principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de micrófono
                FloatingActionButton(
                    onClick = {
                        isMicrophoneEnabled = !isMicrophoneEnabled
                        onToggleMicrophone()
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (isMicrophoneEnabled) Color(0xFF4CAF50) else Color(0xFFE74C3C),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Micrófono",
                        tint = Color.White
                    )
                }
                
                // Botón de cámara (solo para videollamadas)
                if (callType == "video") {
                    FloatingActionButton(
                        onClick = {
                            isCameraEnabled = !isCameraEnabled
                            onToggleCamera()
                        },
                        modifier = Modifier.size(56.dp),
                        containerColor = if (isCameraEnabled) Color(0xFF4CAF50) else Color(0xFFE74C3C),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Cámara",
                            tint = Color.White
                        )
                    }
                }
                
                // Botón de colgar (rojo, más grande)
                FloatingActionButton(
                    onClick = onEndCall,
                    modifier = Modifier.size(64.dp),
                    containerColor = Color(0xFFE74C3C),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Colgar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Texto de estado
            Text(
                text = "Conectando...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}