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
    START_AUDIO_RECORDING,
    START_VIDEO_RECORDING,
    SEND_EMERGENCY_ALERT,
    START_GROUP_CALL,
    SEND_LOCATION,
    TAKE_PHOTO,
    SEND_MESSAGE,
    ACTIVATE_STEALTH_MODE,
    CUSTOM_ACTION
}

enum class CommandPriority {
    LOW, NORMAL, HIGH, CRITICAL
}