package com.example.demoappchat.domain.usecase.voice

import com.example.demoappchat.domain.repository.VoiceRepository
import javax.inject.Inject

/**
 * Use Case para iniciar el reconocimiento de voz
 */
class StartVoiceRecognitionUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    
    suspend fun execute(): Result<Unit> {
        return try {
            voiceRepository.startVoiceRecognition()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}