package com.example.demoappchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import org.vosk.LibVosk
import org.vosk.LogLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltAndroidApp
class MyApplication : Application() {

    private var isVoskInitialized = false

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Inicializar Vosk
        initializeVosk()

        // Crear canales de notificación
        createNotificationChannels()

        // Extraer modelo de Vosk en segundo plano (solo si Vosk se inicializó)
        if (isVoskInitialized) {
            Thread {
                extractVoskModel()
            }.start()
        }
    }

    private fun initializeVosk() {
        try {
            // Configurar nivel de log de Vosk
            LibVosk.setLogLevel(LogLevel.INFO)

            // Configurar directorio de modelos
            val modelDir = File(filesDir, "vosk-models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            isVoskInitialized = true
            Log.d("MyApplication", "✅ Vosk initialized successfully")

        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Error initializing Vosk", e)
            isVoskInitialized = false
        }
    }

    private fun extractVoskModel() {
        try {
            val modelDir = File(filesDir, "vosk-models/spanish")
            if (!modelDir.exists()) {
                modelDir.mkdirs()

                // ✅ Lista actualizada basada en TU modelo actual
                val modelFiles = listOf(
                    // Directorio am/
                    "am/final.mdl",

                    // Directorio conf/
                    "conf/mfcc.conf",
                    "conf/model.conf",

                    // Directorio graph/
                    "graph/disambig_tid.int",
                    "graph/Gr.fst",
                    "graph/HCLr.fst",

                    // Directorio ivector/
                    "ivector/final.dubm",
                    "ivector/final.ie",
                    "ivector/final.mat",
                    "ivector/global_cmvn.stats",
                    "ivector/online_cmvn.conf",
                    "ivector/splice.conf"
                )

                // Copiar archivos del modelo desde assets
                var copiedFiles = 0
                for (file in modelFiles) {
                    try {
                        copyAssetFile("vosk-model/$file", File(modelDir, file))
                        copiedFiles++
                        Log.d("MyApplication", "✅ Copied: $file")
                    } catch (e: IOException) {
                        Log.w("MyApplication", "⚠️ Could not copy: $file", e)
                    }
                }

                // Copiar carpeta phones si existe
                try {
                    copyPhonesDirectory(modelDir)
                } catch (e: Exception) {
                    Log.w("MyApplication", "⚠️ Could not copy phones directory", e)
                }

                Log.d("MyApplication", "✅ Vosk model extraction completed. Files copied: $copiedFiles/${modelFiles.size}")
            } else {
                Log.d("MyApplication", "✅ Vosk model already exists")
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Error extracting Vosk model", e)
        }
    }

    private fun copyPhonesDirectory(modelDir: File) {
        try {
            val phonesDir = File(modelDir, "graph/phones")
            phonesDir.mkdirs()

            // Listar archivos en la carpeta phones
            val phoneFiles = assets.list("vosk-model/graph/phones") ?: return

            for (phoneFile in phoneFiles) {
                copyAssetFile("vosk-model/graph/phones/$phoneFile", File(phonesDir, phoneFile))
                Log.d("MyApplication", "✅ Copied phone file: $phoneFile")
            }
        } catch (e: Exception) {
            Log.w("MyApplication", "⚠️ Error copying phones directory", e)
        }
    }

    private fun copyAssetFile(assetPath: String, destFile: File) {
        try {
            destFile.parentFile?.mkdirs()

            assets.open(assetPath).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw e // Re-throw para manejar en el método padre
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para el servicio de reconocimiento de voz
            val voiceChannel = NotificationChannel(
                VOICE_CHANNEL_ID,
                "Reconocimiento de Voz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de escucha en segundo plano"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Canal para alertas de emergencia
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Alertas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de emergencia activadas por voz"
                enableVibration(true)
                enableLights(true)
            }

            // Canal para mensajes de chat
            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "Mensajes de Chat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mensajes de chats de seguridad"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(
                voiceChannel,
                emergencyChannel,
                chatChannel
            ))
        }
    }

    companion object {
        const val VOICE_CHANNEL_ID = "voice_recognition_channel"
        const val EMERGENCY_CHANNEL_ID = "emergency_alerts_channel"
        const val CHAT_CHANNEL_ID = "chat_messages_channel"
    }
}