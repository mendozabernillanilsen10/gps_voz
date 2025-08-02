package com.example.demoappchat.domain.usecase.voice

import com.example.demoappchat.domain.repository.VoiceRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Case para detener el reconocimiento de voz
 * Maneja la lógica de negocio para finalizar el servicio de voz
 */
@Singleton
class StopVoiceRecognitionUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    
    /**
     * Detiene el reconocimiento de voz de forma segura
     */
    suspend fun execute(): Result<Unit> {
        return try {
            android.util.Log.d("StopVoiceRecognition", "🛑 Deteniendo reconocimiento de voz...")
            
            // Detener reconocimiento usando el repositorio
            val result = voiceRepository.stopVoiceRecognition()
            
            if (result.isSuccess) {
                android.util.Log.d("StopVoiceRecognition", "✅ Reconocimiento detenido exitosamente")
            } else {
                android.util.Log.e("StopVoiceRecognition", "❌ Error deteniendo reconocimiento")
            }
            
            result
            
        } catch (e: Exception) {
            android.util.Log.e("StopVoiceRecognition", "❌ Excepción deteniendo reconocimiento", e)
            Result.failure(e)
        }
    }
}