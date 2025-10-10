package com.example.demoappchat.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.demoappchat.data.model.WebRTCCallParticipant
import com.example.demoappchat.data.model.WebRTCCallStatus
import com.example.demoappchat.data.model.WebRTCCallType
import com.example.demoappchat.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🎨 ACTIVIDAD DE LLAMADA GRUPAL ESTILO iOS
 * Diseño minimalista con grid de participantes y controles elegantes
 */
class GroupCallActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_CHAT_NAME = "chat_name"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALL_TYPE = "call_type" // "audio" o "video"
        const val EXTRA_IS_INITIATOR = "is_initiator"
        private const val TAG = "GroupCallActivity"
    }
    
    private var chatId: String = ""
    private var chatName: String = ""
    private var callId: String = ""
    private var callType: String = "video"
    private var isInitiator: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener datos del intent
        chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: ""
        chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: "Chat Grupal"
        callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "video"
        isInitiator = intent.getBooleanExtra(EXTRA_IS_INITIATOR, false)
        
        Log.d(TAG, "📞 Iniciando llamada grupal")
        Log.d(TAG, "💬 Chat: $chatName")
        Log.d(TAG, "🎥 Tipo: $callType")
        Log.d(TAG, "👤 Iniciador: $isInitiator")
        
        requestPermissions()
        
        setContent {
            SecurityChatTheme {
                GroupCallScreen(
                    chatName = chatName,
                    callType = callType,
                    isInitiator = isInitiator,
                    onEndCall = { endCall() }
                )
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        
        if (callType == "video") {
            permissions.add(Manifest.permission.CAMERA)
        }
        
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
            Log.d(TAG, "✅ Permisos concedidos")
            // TODO: Iniciar WebRTC
        } else {
            Log.w(TAG, "⚠️ Permisos denegados")
            finish()
        }
    }
    
    private fun endCall() {
        Log.d(TAG, "📞 Finalizando llamada")
        // TODO: Limpiar recursos WebRTC
        finish()
    }
}

/**
 * 🎨 PANTALLA PRINCIPAL DE LLAMADA GRUPAL
 * Estilo iOS minimalista
 */
@Composable
fun GroupCallScreen(
    chatName: String,
    callType: String,
    isInitiator: Boolean,
    onEndCall: () -> Unit
) {
    // Estados
    var isMicEnabled by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(callType == "video") }
    var isSpeakerEnabled by remember { mutableStateOf(false) }
    var callStatus by remember { mutableStateOf(WebRTCCallStatus.CONNECTING) }
    
    // Participantes de ejemplo (en producción vendrían de WebRTC)
    val participants by remember {
        mutableStateOf(listOf(
            WebRTCCallParticipant(
                userId = "1",
                userName = "Juan Pérez",
                isMuted = false,
                isVideoEnabled = true
            ),
            WebRTCCallParticipant(
                userId = "2",
                userName = "María García",
                isMuted = false,
                isVideoEnabled = true
            ),
            WebRTCCallParticipant(
                userId = "3",
                userName = "Pedro López",
                isMuted = true,
                isVideoEnabled = false
            )
        ))
    }
    
    // Simular conexión
    LaunchedEffect(Unit) {
        delay(2000)
        callStatus = WebRTCCallStatus.CONNECTED
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E), // iOS dark background
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 🔝 BARRA SUPERIOR
            IOSTopBar(
                chatName = chatName,
                callStatus = callStatus,
                participantCount = participants.size
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 📹 GRID DE PARTICIPANTES
            ParticipantGrid(
                participants = participants,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 🎛️ CONTROLES IOS-STYLE
            IOSCallControls(
                isMicEnabled = isMicEnabled,
                isCameraEnabled = isCameraEnabled,
                isSpeakerEnabled = isSpeakerEnabled,
                callType = callType,
                onToggleMic = { isMicEnabled = !isMicEnabled },
                onToggleCamera = { isCameraEnabled = !isCameraEnabled },
                onToggleSpeaker = { isSpeakerEnabled = !isSpeakerEnabled },
                onEndCall = onEndCall,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

/**
 * 🔝 BARRA SUPERIOR ESTILO iOS
 */
@Composable
fun IOSTopBar(
    chatName: String,
    callStatus: WebRTCCallStatus,
    participantCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E).copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre del chat
        Text(
            text = chatName,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Estado de la llamada
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Indicador animado
            if (callStatus == WebRTCCallStatus.CONNECTING) {
                PulsingDot()
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = when (callStatus) {
                    WebRTCCallStatus.CONNECTING -> "Conectando..."
                    WebRTCCallStatus.CONNECTED -> "$participantCount participante${if (participantCount != 1) "s" else ""}"
                    WebRTCCallStatus.RINGING -> "Llamando..."
                    else -> "Finalizada"
                },
                color = Color(0xFF8E8E93), // iOS gray
                fontSize = 15.sp
            )
        }
    }
}

/**
 * 📹 GRID DE PARTICIPANTES
 * Grid adaptable según cantidad de participantes
 */
@Composable
fun ParticipantGrid(
    participants: List<WebRTCCallParticipant>,
    modifier: Modifier = Modifier
) {
    val columns = when {
        participants.size <= 1 -> 1
        participants.size <= 4 -> 2
        else -> 3
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(participants) { participant ->
            ParticipantCard(participant)
        }
    }
}

/**
 * 👤 TARJETA DE PARTICIPANTE ESTILO iOS
 */
@Composable
fun ParticipantCard(participant: WebRTCCallParticipant) {
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = if (!participant.isMuted) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .scale(if (!participant.isMuted) scale else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color(0xFF1C1C1E)
                    )
                )
            )
            .shadow(
                elevation = if (!participant.isMuted) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (!participant.isMuted) Color(0xFF007AFF).copy(alpha = 0.3f) else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF007AFF), // iOS blue
                                Color(0xFF0051D5)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.userName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Nombre
            Text(
                text = participant.userName.split(" ").firstOrNull() ?: "Usuario",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Indicadores de estado
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Micrófono
                if (participant.isMuted) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = "Muted",
                        tint = Color(0xFFFF3B30), // iOS red
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Video
                if (!participant.isVideoEnabled) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = "Video off",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 🎛️ CONTROLES DE LLAMADA ESTILO iOS
 * Con efecto blur y botones circulares
 */
@Composable
fun IOSCallControls(
    isMicEnabled: Boolean,
    isCameraEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    callType: String,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Controles secundarios
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Altavoz
            IOSControlButton(
                icon = if (isSpeakerEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                label = "Altavoz",
                isActive = isSpeakerEnabled,
                onClick = onToggleSpeaker,
                size = 56.dp
            )
            
            // Cámara (solo para video)
            if (callType == "video") {
                IOSControlButton(
                    icon = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    label = "Cámara",
                    isActive = isCameraEnabled,
                    onClick = onToggleCamera,
                    size = 56.dp
                )
            }
            
            // Micrófono
            IOSControlButton(
                icon = if (isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                label = "Micro",
                isActive = isMicEnabled,
                onClick = onToggleMic,
                size = 56.dp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Botón de colgar (más grande y destacado)
        IOSHangupButton(onClick = onEndCall)
    }
}

/**
 * 🔘 BOTÓN DE CONTROL ESTILO iOS
 */
@Composable
fun IOSControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    size: Dp = 56.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isActive) 8.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = if (isActive) Color(0xFF007AFF).copy(alpha = 0.3f) else Color.Transparent
                ),
            containerColor = if (isActive) Color(0xFF007AFF) else Color(0xFF3A3A3C), // iOS colors
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color(0xFF8E8E93),
            fontSize = 12.sp
        )
    }
}

/**
 * ☎️ BOTÓN DE COLGAR ESTILO iOS
 * Rojo destacado y más grande
 */
@Composable
fun IOSHangupButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = Color(0xFFFF3B30).copy(alpha = 0.4f)
            ),
        containerColor = Color(0xFFFF3B30), // iOS red
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = "Colgar",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * ⚫ DOT PULSANTE
 * Indicador animado para "conectando"
 */
@Composable
fun PulsingDot() {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF007AFF).copy(alpha = alpha))
    )
}

