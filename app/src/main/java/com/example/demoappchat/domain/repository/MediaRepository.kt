package com.example.demoappchat.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para manejo de multimedia (audio, video, fotos)
 */
interface MediaRepository {
    
    /**
     * Inicia grabación de audio
     */
    suspend fun startAudioRecording(): Result<String>
    
    /**
     * Detiene grabación de audio
     */
    suspend fun stopAudioRecording(): Result<String>
    
    /**
     * Inicia grabación de video
     */
    suspend fun startVideoRecording(): Result<String>
    
    /**
     * Detiene grabación de video
     */
    suspend fun stopVideoRecording(): Result<String>
    
    /**
     * Toma una foto
     */
    suspend fun takePhoto(): Result<String>
    
    /**
     * Verifica si está grabando audio
     */
    fun isRecordingAudio(): Flow<Boolean>
    
    /**
     * Verifica si está grabando video
     */
    fun isRecordingVideo(): Flow<Boolean>
    
    /**
     * Obtiene la duración de grabación actual
     */
    fun getRecordingDuration(): Flow<Long>
    
    /**
     * Sube archivo multimedia a Firebase Storage
     */
    suspend fun uploadMediaFile(filePath: String, chatId: String): Result<String>
    
    /**
     * Elimina archivo multimedia local
     */
    suspend fun deleteLocalFile(filePath: String): Result<Unit>
}