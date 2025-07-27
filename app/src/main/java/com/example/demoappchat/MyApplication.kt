package com.example.demoappchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    
    private val modelExtractionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.demoappchat.EXTRACT_VOSK_MODEL") {
                Log.d("MyApplication", "🔄 Recibida solicitud de extracción de modelo")
                Thread {
                    extractVoskModel()
                }.start()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Registrar receptor para extracción de modelo
        registerReceiver(modelExtractionReceiver, IntentFilter("com.example.demoappchat.EXTRACT_VOSK_MODEL"))

        // Crear canales de notificación
        createNotificationChannels()

        // Inicializar Vosk y extraer modelo en background
        Thread {
            initializeVosk()
            if (isVoskInitialized) {
                Log.d("MyApplication", "🔄 Iniciando extracción del modelo en background...")
                extractVoskModel()
            }
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

            isVoskInitialized = true
            Log.d("MyApplication", "✅ Vosk initialized successfully")

        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Error initializing Vosk", e)
            isVoskInitialized = false
        }
    }

    private fun extractVoskModel() {
        synchronized(this) {
            try {
                val modelDir = File(filesDir, "vosk-models/spanish")
                Log.d("MyApplication", "🔍 Verificando modelo en: ${modelDir.absolutePath}")
                
                // Verificar si ya existe y está completo
                if (modelDir.exists() && isModelComplete(modelDir)) {
                    Log.d("MyApplication", "✅ Modelo Vosk ya está completo")
                    return
                }
                
                // Si no existe o está incompleto, extraer
                Log.d("MyApplication", "📦 Extrayendo modelo Vosk...")
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
                        Log.d("MyApplication", "✅ Copiado: $file")
                    } catch (e: IOException) {
                        Log.e("MyApplication", "❌ No se pudo copiar: $file", e)
                    }
                }

                // Copiar carpeta phones si existe
                try {
                    copyPhonesDirectory(modelDir)
                } catch (e: Exception) {
                    Log.w("MyApplication", "⚠️ No se pudo copiar directorio phones", e)
                }

                Log.d("MyApplication", "✅ Extracción del modelo completada. Archivos copiados: $copiedFiles/${modelFiles.size}")
                
                // Verificar extracción
                if (isModelComplete(modelDir)) {
                    Log.d("MyApplication", "🎉 Modelo Vosk extraído y verificado correctamente")
                } else {
                    Log.e("MyApplication", "❌ Extracción incompleta del modelo")
                }
                
            } catch (e: Exception) {
                Log.e("MyApplication", "❌ Error extrayendo modelo Vosk", e)
            }
        }
    }
    
    private fun isModelComplete(modelDir: File): Boolean {
        val requiredFiles = listOf(
            "am/final.mdl",
            "conf/mfcc.conf", 
            "conf/model.conf",
            "graph/Gr.fst",
            "graph/HCLr.fst"
        )
        
        for (file in requiredFiles) {
            val fileObj = File(modelDir, file)
            if (!fileObj.exists() || fileObj.length() == 0L) {
                Log.w("MyApplication", "Archivo faltante o vacío: $file")
                return false
            }
        }
        return true
    }
    
    override fun onTerminate() {
        super.onTerminate()
        try {
            unregisterReceiver(modelExtractionReceiver)
        } catch (e: Exception) {
            // Ignorar errores al desregistrar
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
        
        private var instance: MyApplication? = null
        
        fun getInstance(): MyApplication? = instance
        
        fun forceModelExtraction() {
            instance?.extractVoskModel()
        }
    }
    
    init {
        instance = this
    }
}