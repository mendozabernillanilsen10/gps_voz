package com.example.demoappchat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.ui.theme.*

/**
 * Barra de estado del servicio de voz para chats grupales
 * Muestra el estado actual y permite controlar el servicio
 */
@Composable
fun VoiceServiceStatusBar(
    isActive: Boolean,
    status: String,
    isGroupChat: Boolean,
    onToggleService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isGroupChat) return // Solo mostrar en chats grupales
    
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) SafetyGreen.copy(alpha = 0.1f) else Color(0xFFF0F2F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y estado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icono simple (sin animación experimental)
                    Icon(
                        imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isActive) "Micrófono activo" else "Micrófono inactivo",
                        tint = if (isActive) SafetyGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // Texto de estado
                    Column {
                        Text(
                            text = if (isActive) "Servicio de Voz Activo" else "Servicio de Voz Inactivo",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isActive) SafetyGreen else Color.Gray
                        )
                        
                        Text(
                            text = status,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
                
                // Switch de control
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafetyGreen,
                        checkedTrackColor = SafetyGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

/**
 * Indicador compacto del servicio de voz
 */
@Composable
fun VoiceServiceIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return
    
    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = SafetyGreen,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/**
 * Botón flotante para activar/desactivar servicio de voz
 */
@Composable
fun VoiceServiceFloatingButton(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier,
        containerColor = if (isActive) SafetyGreen else Color.Gray,
        contentColor = Color.White
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isActive) "Desactivar servicio" else "Activar servicio"
        )
    }
} 