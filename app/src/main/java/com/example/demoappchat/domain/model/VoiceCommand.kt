package com.example.demoappchat.domain.model

/**
 * Modelo de dominio para comandos de voz
 * Arquitectura Clean - Capa de Dominio
 */
data class VoiceCommand(
    val id: String,
    val phrase: String,
    val action: VoiceAction,
    val isEnabled: Boolean = true,
    val priority: CommandPriority = CommandPriority.NORMAL,
    val requiredConfidence: Float = 0.7f,
    val metadata: Map<String, String> = emptyMap()
)

enum class VoiceAction {
    START_AUDIO_RECORDING,    // Grabar audio
    START_VIDEO_RECORDING,    // Grabar video
    SEND_EMERGENCY_ALERT,     // Enviar alerta de emergencia
    START_GROUP_CALL,         // Iniciar llamada grupal
    SEND_LOCATION,            // Enviar ubicación
    TAKE_PHOTO,               // Tomar foto
    SEND_MESSAGE,             // Enviar mensaje de texto
    ACTIVATE_STEALTH_MODE,    // Activar modo sigiloso
    SEND_STATUS_UPDATE,       // Enviar actualización de estado
    START_TRACKING,           // Iniciar seguimiento de ubicación
    STOP_TRACKING,            // Detener seguimiento
    SEND_AUDIO_MESSAGE,       // Enviar mensaje de audio
    SEND_VIDEO_MESSAGE,       // Enviar mensaje de video
    SEND_PHOTO_MESSAGE,       // Enviar mensaje con foto
    ACTIVATE_SURVEILLANCE,    // Activar modo vigilancia
    DEACTIVATE_SURVEILLANCE,  // Desactivar modo vigilancia
    SEND_SOS,                 // Enviar señal SOS
    CUSTOM_ACTION             // Acción personalizada
}

enum class CommandPriority {
    LOW, NORMAL, HIGH, CRITICAL
}