package com.example.demoappchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import org.vosk.LibVosk
import org.vosk.LogLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Inicializar Vosk
        initializeVosk()

        // Crear canales de notificación
        createNotificationChannels()

        // Extraer modelo de Vosk en segundo plano
        Thread {
            extractVoskModel()
        }.start()
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractVoskModel() {
        try {
            val modelDir = File(filesDir, "vosk-models/spanish")
            if (!modelDir.exists()) {
                modelDir.mkdirs()

                // Lista de archivos del modelo que necesitas copiar desde assets
                val modelFiles = listOf(
                    "am/final.mdl",
                    "graph/HCLG.fst",
                    "graph/phones.txt",
                    "graph/words.txt",
                    "ivector/final.dubm",
                    "ivector/final.ie",
                    "ivector/final.mat",
                    "ivector/global_cmvn.stats",
                    "ivector/online_cmvn.conf",
                    "ivector/splice.conf",
                    "conf/mfcc.conf",
                    "conf/model.conf"
                )

                // Copiar archivos del modelo desde assets
                for (file in modelFiles) {
                    copyAssetFile("vosk-model/$file", File(modelDir, file))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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