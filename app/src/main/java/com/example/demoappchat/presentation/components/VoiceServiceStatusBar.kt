package com.example.demoappchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceServiceStatusBar(
    isActive: Boolean,
    status: String,
    isGroupChat: Boolean = false,
    onToggleService: (Boolean) -> Unit
) {
    if (isGroupChat) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isActive) "Micrófono activo" else "Micrófono inactivo",
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column {
                        Text(
                            text = if (isActive) "Servicio de voz activo" else "Servicio de voz inactivo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = status,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                color = MaterialTheme.colorScheme.primary,
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
        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isActive) "Desactivar servicio" else "Activar servicio"
        )
    }
} 