package com.example.demoappchat.domain.model

/**
 * Resultado de reconocimiento de voz con metadatos completos
 */
data class VoiceRecognitionResult(
    val text: String,
    val confidence: Float,
    val timestamp: Long,
    val source: RecognitionSource,
    val matchedCommand: VoiceCommand? = null,
    val processingTimeMs: Long = 0L
)

/**
 * Fuente del reconocimiento de voz
 */
enum class RecognitionSource {
    ANDROID_SPEECH,
    VOSK,
    AUDIO_PATTERN
}

/**
 * Alias para facilitar la compatibilidad
 */
typealias EngineType = RecognitionSource