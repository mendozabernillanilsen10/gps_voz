package com.example.demoappchat.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.demoappchat.data.service.BackgroundVoiceService

/**
 * Receptor para comandos de voz del sistema
 * Activa el servicio de fondo cuando se detecta un comando
 */
class VoiceCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("VoiceCommandReceiver", "🎤 Comando de voz recibido: ${intent.action}")
        
        when (intent.action) {
            "android.intent.action.VOICE_COMMAND",
            "android.media.action.VOICE_COMMAND",
            "com.example.demoappchat.VOICE_COMMAND" -> {
                handleVoiceCommand(context, intent)
            }
            Intent.ACTION_SCREEN_OFF -> {
                // Activar servicio de fondo cuando se apaga la pantalla
                activateBackgroundVoiceService(context)
            }
            Intent.ACTION_SCREEN_ON -> {
                // Continuar servicio cuando se enciende la pantalla
                ensureBackgroundServiceRunning(context)
            }
        }
    }

    private fun handleVoiceCommand(context: Context, intent: Intent) {
        try {
            Log.d("VoiceCommandReceiver", "🎯 Procesando comando de voz del sistema")
            
            // Extraer información del comando
            val commandText = intent.getStringExtra("command_text") ?: ""
            val audioData = intent.getByteArrayExtra("audio_data")
            val commandType = detectCommandType(commandText)
            
            Log.d("VoiceCommandReceiver", "📝 Comando detectado: '$commandText' - Tipo: $commandType")
            
            // Activar servicio de fondo si no está corriendo
            activateBackgroundVoiceService(context)
            
            // Enviar comando al servicio
            val serviceIntent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = BackgroundVoiceService.ACTION_CREATE_CHAT
                putExtra("chat_type", commandType)
                audioData?.let { putExtra("audio_data", it) }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d("VoiceCommandReceiver", "✅ Comando enviado al servicio de fondo")
            
        } catch (e: Exception) {
            Log.e("VoiceCommandReceiver", "❌ Error procesando comando de voz", e)
        }
    }

    private fun detectCommandType(commandText: String): String {
        return when {
            commandText.contains("emergencia", ignoreCase = true) ||
            commandText.contains("ayuda", ignoreCase = true) ||
            commandText.contains("socorro", ignoreCase = true) -> "emergency"
            
            commandText.contains("vigilancia", ignoreCase = true) ||
            commandText.contains("observar", ignoreCase = true) ||
            commandText.contains("monitorear", ignoreCase = true) -> "surveillance"
            
            commandText.contains("grabar", ignoreCase = true) ||
            commandText.contains("audio", ignoreCase = true) ||
            commandText.contains("sonido", ignoreCase = true) -> "recording"
            
            commandText.contains("chat", ignoreCase = true) ||
            commandText.contains("grupo", ignoreCase = true) ||
            commandText.contains("conversar", ignoreCase = true) -> "general"
            
            else -> "emergency" // Por defecto, emergencia
        }
    }

    private fun activateBackgroundVoiceService(context: Context) {
        try {
            Log.d("VoiceCommandReceiver", "🚀 Activando servicio de voz en segundo plano")
            
            val serviceIntent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = BackgroundVoiceService.ACTION_START_BACKGROUND
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d("VoiceCommandReceiver", "✅ Servicio de fondo activado")
            
        } catch (e: Exception) {
            Log.e("VoiceCommandReceiver", "❌ Error activando servicio de fondo", e)
        }
    }

    private fun ensureBackgroundServiceRunning(context: Context) {
        try {
            // Verificar si el servicio está corriendo
            val isRunning = isServiceRunning(context, BackgroundVoiceService::class.java)
            
            if (!isRunning) {
                Log.d("VoiceCommandReceiver", "🔄 Servicio no está corriendo, reactivando...")
                activateBackgroundVoiceService(context)
            } else {
                Log.d("VoiceCommandReceiver", "✅ Servicio de fondo ya está corriendo")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceCommandReceiver", "❌ Error verificando servicio", e)
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
        
        return runningServices.any { it.service.className == serviceClass.name }
    }

    companion object {
        fun register(context: Context) {
            try {
                val receiver = VoiceCommandReceiver()
                val filter = IntentFilter().apply {
                    addAction("android.intent.action.VOICE_COMMAND")
                    addAction("android.media.action.VOICE_COMMAND")
                    addAction("com.example.demoappchat.VOICE_COMMAND")
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
                
                context.registerReceiver(receiver, filter)
                Log.d("VoiceCommandReceiver", "📡 Receptor de comandos de voz registrado")
                
            } catch (e: Exception) {
                Log.e("VoiceCommandReceiver", "❌ Error registrando receptor", e)
            }
        }

        fun unregister(context: Context, receiver: VoiceCommandReceiver) {
            try {
                context.unregisterReceiver(receiver)
                Log.d("VoiceCommandReceiver", "📡 Receptor de comandos de voz desregistrado")
            } catch (e: Exception) {
                Log.e("VoiceCommandReceiver", "❌ Error desregistrando receptor", e)
            }
        }
    }
}
