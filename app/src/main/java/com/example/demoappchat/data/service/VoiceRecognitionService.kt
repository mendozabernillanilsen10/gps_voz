package com.example.demoappchat.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.CamcorderProfile
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.*
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
    
    // Variables para grabación de audio/video
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    private var recordingType: RecordingType = RecordingType.NONE

    // CoroutineScope para manejar operaciones suspendidas
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    enum class RecordingType {
        NONE, AUDIO, VIDEO
    }

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
            ACTION_START_RECORDING -> {
                val type = intent.getStringExtra(EXTRA_RECORDING_TYPE) ?: "AUDIO"
                startRecording(if (type == "VIDEO") RecordingType.VIDEO else RecordingType.AUDIO)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
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
        Log.d("VoiceService", "Comando detectado: $recognizedText")

        // Verificar si hay chat activo
        val currentChatId = preferences.currentChatId
        if (currentChatId.isNullOrEmpty()) {
            // Mostrar notificación de advertencia
            val warningNotification = NotificationCompat.Builder(this, MyApplication.VOICE_CHANNEL_ID)
                .setContentTitle("SafeVoice: No estás en un grupo")
                .setContentText("Únete a un chat para activar la grabación por voz.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(EMERGENCY_NOTIFICATION_ID + 1, warningNotification)
            return
        }

        // Determinar tipo de grabación según el comando
        val recordingType = when {
            recognizedText.contains("grabar audio") || recognizedText.contains("audio") -> RecordingType.AUDIO
            recognizedText.contains("grabar video") || recognizedText.contains("video") || recognizedText.contains("cámara") -> RecordingType.VIDEO
            else -> RecordingType.AUDIO // Por defecto grabar audio
        }

        // Iniciar grabación
        startRecording(recordingType)

        if (preferences.discreteMode) {
            // Modo discreto: sin notificaciones visibles
            sendSilentEmergencyAlert(recognizedText, recordingType)
        } else {
            // Modo normal: con notificaciones
            sendEmergencyAlert(recognizedText, recordingType)
        }
    }

    private fun startRecording(type: RecordingType) {
        if (isRecording) {
            stopRecording()
        }
        
        try {
            when (type) {
                RecordingType.AUDIO -> startAudioRecording()
                RecordingType.VIDEO -> startVideoRecording()
                RecordingType.NONE -> return
            }
            
            recordingType = type
            isRecording = true
            
            // Actualizar notificación
            updateNotification()
            
            Log.d("VoiceService", "Grabación iniciada: ${type.name}")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "Error iniciando grabación", e)
        }
    }

    private fun startAudioRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_$timestamp.mp3"
        currentRecordingFile = File(getExternalFilesDir(null), fileName)
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentRecordingFile?.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("VoiceService", "Error preparando grabación de audio", e)
            }
        }
    }

    private fun startVideoRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "video_$timestamp.mp4"
        currentRecordingFile = File(getExternalFilesDir(null), fileName)
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            setOutputFile(currentRecordingFile?.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("VoiceService", "Error preparando grabación de video", e)
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            isRecording = false
            
            // Subir archivo a Firebase Storage
            currentRecordingFile?.let { file ->
                uploadFileToFirebase(file, recordingType)
            }
            
            currentRecordingFile = null
            recordingType = RecordingType.NONE
            
            // Actualizar notificación
            updateNotification()
            
            Log.d("VoiceService", "Grabación detenida")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "Error deteniendo grabación", e)
        }
    }

    // En VoiceRecognitionService.kt, actualiza este método:
    private fun uploadFileToFirebase(file: File, type: RecordingType) {
        serviceScope.launch {
            try {
                val chatId = preferences.currentChatId ?: return@launch
                val mediaType = if (type == RecordingType.AUDIO) "audio" else "video"

                // ✅ USAR EL REPOSITORY INYECTADO
                val downloadUrl = repository.uploadMediaFile(file, mediaType, chatId)

                // Enviar mensaje al chat con el archivo
                repository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = downloadUrl,
                    messageType = if (type == RecordingType.AUDIO) "AUDIO" else "VIDEO",
                    content = "Grabación automática: ${type.name.lowercase()}"
                )

                Log.d("VoiceService", "Archivo subido exitosamente: $downloadUrl")

            } catch (e: Exception) {
                Log.e("VoiceService", "Error subiendo archivo", e)
            }
        }
    }

    private fun sendSilentEmergencyAlert(command: String, type: RecordingType) {
        serviceScope.launch {
            try {
                repository.sendEmergencyAlert(command, preferences.emergencyRadius, type.name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendEmergencyAlert(command: String, type: RecordingType) {
        // Mostrar notificación de emergencia
        val emergencyNotification = NotificationCompat.Builder(this, MyApplication.EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 Alerta Activada")
            .setContentText("Comando: \"$command\" - Grabando: ${type.name}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EMERGENCY_NOTIFICATION_ID, emergencyNotification)

        sendSilentEmergencyAlert(command, type)
    }

    // Refuerzo la creación de la notificación foreground
    private fun createNotification(): Notification {
        val channelId = MyApplication.VOICE_CHANNEL_ID
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SafeVoice Servicio de Voz",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SafeVoice Escuchando")
            .setContentText(getNotificationText())
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(createStopAction())
            .setContentIntent(createMainActivityIntent())
            .build()
    }

    private fun getNotificationText(): String {
        return when {
            isRecording -> "Grabando: ${recordingType.name.lowercase()}"
            else -> "Comandos: ${preferences.activationCommands.joinToString(", ")}"
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, VoiceRecognitionService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
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
        stopRecording()
        recognizer?.close()
        model?.close()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_LISTENING = "com.example.demoappchat.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.demoappchat.STOP_LISTENING"
        const val ACTION_START_RECORDING = "com.example.demoappchat.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.demoappchat.STOP_RECORDING"
        const val EXTRA_RECORDING_TYPE = "recording_type"

        private const val NOTIFICATION_ID = 1001
        private const val EMERGENCY_NOTIFICATION_ID = 1002
    }
}