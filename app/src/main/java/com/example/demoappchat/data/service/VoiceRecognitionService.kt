package com.example.demoappchat.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.demoappchat.MainActivity
import com.example.demoappchat.MyApplication
import com.example.demoappchat.R
import com.example.demoappchat.data.VoiceServicePreferences
import com.example.demoappchat.data.repository.FirebaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    @Inject
    lateinit var repository: FirebaseRepository

    @Inject
    lateinit var preferences: VoiceServicePreferences

    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null
    private var isListening = false
    private var wakeLock: PowerManager.WakeLock? = null

    // CoroutineScope para manejar operaciones suspendidas
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        initializeVosk()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startListening()
                preferences.isVoiceServiceEnabled = true
            }
            ACTION_STOP_LISTENING -> {
                stopListening()
                preferences.isVoiceServiceEnabled = false
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startListening()
            }
        }

        return START_STICKY // Reinicia automáticamente
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SafeVoice::VoiceRecognitionWakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }

    private fun initializeVosk() {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS)

            val modelDir = File(filesDir, "vosk-models/spanish")
            if (modelDir.exists()) {
                model = Model(modelDir.absolutePath)
                recognizer = Recognizer(model, 16000.0f)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startListening() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isListening = true

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isListening) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    recognizer?.acceptWaveForm(buffer, bytesRead)?.let { isEndOfSpeech ->
                        if (isEndOfSpeech) {
                            handleSpeechResult()
                        }
                    }
                }
                Thread.sleep(100) // Pequeña pausa para no saturar CPU
            }
        }.start()
    }

    private fun stopListening() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun handleSpeechResult() {
        recognizer?.finalResult?.let { result ->
            try {
                val jsonResult = JSONObject(result)
                val text = jsonResult.getString("text").lowercase()

                // Verificar comandos de activación configurados
                val activationCommands = preferences.activationCommands
                if (activationCommands.any { command -> text.contains(command) }) {
                    triggerEmergencyAction(text)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerEmergencyAction(recognizedText: String) {
        if (preferences.discreteMode) {
            // Modo discreto: sin notificaciones visibles
            sendSilentEmergencyAlert(recognizedText)
        } else {
            // Modo normal: con notificaciones
            sendEmergencyAlert(recognizedText)
        }
    }

    private fun sendSilentEmergencyAlert(command: String) {
        // Usar CoroutineScope para llamar función suspend
        serviceScope.launch {
            try {
                repository.sendEmergencyAlert(command, preferences.emergencyRadius)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendEmergencyAlert(command: String) {
        // Mostrar notificación de emergencia
        val emergencyNotification = NotificationCompat.Builder(this, MyApplication.EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 Alerta Activada")
            .setContentText("Comando: \"$command\" detectado")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Usar icono del sistema
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EMERGENCY_NOTIFICATION_ID, emergencyNotification)

        sendSilentEmergencyAlert(command)
    }

    private fun createNotification() = NotificationCompat.Builder(this, MyApplication.VOICE_CHANNEL_ID)
        .setContentTitle("SafeVoice Escuchando")
        .setContentText("Comandos: ${preferences.activationCommands.joinToString(", ")}")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Usar icono del sistema
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .addAction(createStopAction())
        .setContentIntent(createMainActivityIntent())
        .build()

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, VoiceRecognitionService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action(
            android.R.drawable.ic_media_pause, // Usar icono del sistema
            "Detener",
            stopPendingIntent
        )
    }

    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        recognizer?.close()
        model?.close()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_LISTENING = "com.example.demoappchat.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.demoappchat.STOP_LISTENING"

        private const val NOTIFICATION_ID = 1001
        private const val EMERGENCY_NOTIFICATION_ID = 1002
    }
}