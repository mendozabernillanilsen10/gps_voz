package com.example.demoappchat.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para configuraciones de la aplicación
 */
interface SettingsRepository {
    
    /**
     * Configuraciones de reconocimiento de voz
     */
    suspend fun isVoiceRecognitionEnabled(): Boolean
    suspend fun setVoiceRecognitionEnabled(enabled: Boolean)
    
    suspend fun isStealthModeEnabled(): Boolean
    suspend fun setStealthModeEnabled(enabled: Boolean)
    
    suspend fun getVoiceSensitivity(): Float
    suspend fun setVoiceSensitivity(sensitivity: Float)
    
    /**
     * Configuraciones de grabación
     */
    suspend fun getAutoRecordingDuration(): Int
    suspend fun setAutoRecordingDuration(seconds: Int)
    
    suspend fun isAutoUploadEnabled(): Boolean
    suspend fun setAutoUploadEnabled(enabled: Boolean)
    
    /**
     * Configuraciones de ubicación
     */
    suspend fun getTransmissionRadius(): Float
    suspend fun setTransmissionRadius(radiusKm: Float)
    
    suspend fun isLocationSharingEnabled(): Boolean
    suspend fun setLocationSharingEnabled(enabled: Boolean)
    
    /**
     * Configuraciones de notificaciones
     */
    suspend fun areNotificationsEnabled(): Boolean
    suspend fun setNotificationsEnabled(enabled: Boolean)
    
    suspend fun isDiscreteMode(): Boolean
    suspend fun setDiscreteMode(enabled: Boolean)
    
    /**
     * Configuraciones de chat
     */
    suspend fun getCurrentChatId(): String?
    suspend fun setCurrentChatId(chatId: String?)
    
    /**
     * Observar cambios en configuraciones críticas
     */
    fun observeVoiceSettings(): Flow<VoiceSettings>
    fun observeRecordingSettings(): Flow<RecordingSettings>
}

data class VoiceSettings(
    val isEnabled: Boolean,
    val stealthMode: Boolean,
    val sensitivity: Float
)

data class RecordingSettings(
    val duration: Int,
    val autoUpload: Boolean,
    val quality: String
)