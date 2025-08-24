package com.example.demoappchat.domain.usecase.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.demoappchat.data.service.BackgroundVoiceService
import javax.inject.Inject

/**
 * Use Case para iniciar el servicio de voz en segundo plano
 * Permite que la app detecte comandos de voz incluso cuando está cerrada
 */
class StartBackgroundVoiceServiceUseCase @Inject constructor(
    private val context: Context
) {
    
    fun execute(): Result<Unit> {
        return try {
            // Crear intent para iniciar el servicio
            val serviceIntent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = BackgroundVoiceService.ACTION_START_BACKGROUND
            }
            
            // Iniciar servicio según la versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
