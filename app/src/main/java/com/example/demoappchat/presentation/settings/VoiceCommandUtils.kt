package com.example.demoappchat.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utilidades para comandos de voz
 */
object VoiceCommandUtils {
    
    fun getActionIcon(action: String): ImageVector = when (action) {
        "AUDIO" -> Icons.Default.Mic
        "VIDEO" -> Icons.Default.Videocam
        "PHOTO" -> Icons.Default.CameraAlt
        "TEXT" -> Icons.Default.Message
        "LOCATION" -> Icons.Default.LocationOn
        "CALL" -> Icons.Default.Call
        "SOS" -> Icons.Default.Emergency
        "TRACKING" -> Icons.Default.GpsFixed
        "SURVEILLANCE" -> Icons.Default.Visibility
        "STATUS" -> Icons.Default.Info
        "AUDIO_MESSAGE" -> Icons.Default.Mic
        "VIDEO_MESSAGE" -> Icons.Default.Videocam
        "PHOTO_MESSAGE" -> Icons.Default.CameraAlt
        "AUDIO_RECORDING" -> Icons.Default.Mic
        "STEALTH" -> Icons.Default.VisibilityOff
        else -> Icons.Default.Settings
    }
    
    fun getActionDescription(action: String): String = when (action) {
        "AUDIO" -> "Grabar audio"
        "VIDEO" -> "Grabar video"
        "PHOTO" -> "Capturar foto"
        "TEXT" -> "Enviar mensaje"
        "LOCATION" -> "Compartir ubicación"
        "CALL" -> "Realizar llamada"
        "SOS" -> "Activar emergencia"
        "TRACKING" -> "Iniciar seguimiento"
        "SURVEILLANCE" -> "Activar vigilancia"
        "STATUS" -> "Reportar estado"
        "AUDIO_MESSAGE" -> "Mensaje de audio"
        "VIDEO_MESSAGE" -> "Mensaje de video"
        "PHOTO_MESSAGE" -> "Mensaje con foto"
        "AUDIO_RECORDING" -> "Grabación de audio"
        "STEALTH" -> "Modo sigiloso"
        else -> "Acción personalizada"
    }
}
