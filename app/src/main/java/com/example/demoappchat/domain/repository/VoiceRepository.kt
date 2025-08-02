package com.example.demoappchat.domain.repository

import com.example.demoappchat.domain.model.VoiceCommand
import com.example.demoappchat.domain.model.VoiceRecognitionResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface del repositorio de voz - Capa de Dominio
 * Define los contratos para el manejo de reconocimiento de voz
 */
interface VoiceRepository {
    
    /**
     * Inicia el reconocimiento de voz en segundo plano
     */
    suspend fun startVoiceRecognition(): Result<Unit>
    
    /**
     * Detiene el reconocimiento de voz
     */
    suspend fun stopVoiceRecognition(): Result<Unit>
    
    /**
     * Observa los resultados de reconocimiento en tiempo real
     */
    fun observeVoiceRecognition(): Flow<VoiceRecognitionResult>
    
    /**
     * Configura comandos de voz personalizados
     */
    suspend fun setVoiceCommands(commands: List<VoiceCommand>): Result<Unit>
    
    /**
     * Obtiene comandos de voz configurados
     */
    suspend fun getVoiceCommands(): Result<List<VoiceCommand>>
    
    /**
     * Verifica si el servicio de voz está activo
     */
    fun isVoiceRecognitionActive(): Flow<Boolean>
    
    /**
     * Obtiene el estado del servicio de voz
     */
    fun getVoiceServiceStatus(): Flow<VoiceServiceStatus>
    
    /**
     * Configura el nivel de sensibilidad
     */
    suspend fun setSensitivity(level: Float): Result<Unit>
    
    /**
     * Habilita/deshabilita modo sigiloso
     */
    suspend fun setStealthMode(enabled: Boolean): Result<Unit>
}

data class VoiceServiceStatus(
    val isActive: Boolean,
    val isListening: Boolean,
    val currentEngine: String,
    val batteryOptimized: Boolean,
    val lastCommand: VoiceRecognitionResult?,
    val errorMessage: String?
)