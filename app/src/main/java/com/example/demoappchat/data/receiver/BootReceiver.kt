// data/receiver/BootReceiver.kt
package com.example.demoappchat.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.demoappchat.data.service.VoiceRecognitionService

// QUITAR @AndroidEntryPoint - no es compatible con BroadcastReceiver
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {

                // Usar SharedPreferences directamente sin inyección
                val prefs = context.getSharedPreferences("safevoice_prefs", Context.MODE_PRIVATE)
                val wasVoiceServiceEnabled = prefs.getBoolean("voice_service_enabled", false)

                if (wasVoiceServiceEnabled) {
                    startVoiceRecognitionService(context)
                }
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}