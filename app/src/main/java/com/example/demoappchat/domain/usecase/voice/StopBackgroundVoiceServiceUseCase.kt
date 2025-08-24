package com.example.demoappchat.domain.usecase.voice

import android.content.Context
import android.content.Intent
import com.example.demoappchat.data.service.BackgroundVoiceService
import javax.inject.Inject

/**
 * Use Case para detener el servicio de voz en segundo plano
 */
class StopBackgroundVoiceServiceUseCase @Inject constructor(
    private val context: Context
) {
    
    fun execute(): Result<Unit> {
        return try {
            // Crear intent para detener el servicio
            val serviceIntent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = BackgroundVoiceService.ACTION_STOP_BACKGROUND
            }
            
            // Detener servicio
            context.stopService(serviceIntent)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
