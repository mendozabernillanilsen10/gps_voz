package com.example.demoappchat.domain.usecase.voice

import com.example.demoappchat.domain.model.VoiceRecognitionResult
import com.example.demoappchat.domain.repository.VoiceRepository
import com.example.demoappchat.domain.usecase.voice.ExecuteVoiceCommandUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Case para procesar el reconocimiento de voz
 */
class ProcessVoiceRecognitionUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val executeVoiceCommandUseCase: ExecuteVoiceCommandUseCase
) {
    
    suspend fun execute(result: VoiceRecognitionResult): Result<String> {
        return try {
            // Ejecutar el comando de voz reconocido
            executeVoiceCommandUseCase.execute(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Resultado del procesamiento de voz
 */
sealed class VoiceProcessingResult {
    abstract val originalResult: VoiceRecognitionResult
    
    data class CommandExecuted(
        override val originalResult: VoiceRecognitionResult,
        val executionMessage: String
    ) : VoiceProcessingResult()
    
    data class CommandFailed(
        override val originalResult: VoiceRecognitionResult,
        val errorMessage: String
    ) : VoiceProcessingResult()
    
    data class LowConfidence(
        override val originalResult: VoiceRecognitionResult
    ) : VoiceProcessingResult()
    
    data class ProcessingError(
        override val originalResult: VoiceRecognitionResult,
        val errorMessage: String
    ) : VoiceProcessingResult()
}