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
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.repository.FirebaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    lateinit var preferences: UserPreferences

    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null
    private var isListening = false
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Variables para controlar inicialización única
    private var isInitializing = false
    private var isInitialized = false
    
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
        
        // Verificar si ya hay una instancia ejecutándose
        synchronized(VoiceRecognitionService::class.java) {
            if (isServiceRunning && serviceInstance != null && serviceInstance != this) {
                Log.w("VoiceService", "⚠️ Servicio ya está ejecutándose, cancelando esta instancia")
                stopSelf()
                return
            }
            
            isServiceRunning = true
            serviceInstance = this
            Log.d("VoiceService", "🚀 Iniciando nueva instancia del servicio")
        }
        
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForeground(NOTIFICATION_ID, createNotification())
                serviceScope.launch {
                    preferences.setVoiceServiceEnabled(true)
                    // Solo inicializar Vosk cuando se necesite
                    if (serviceInstance == this@VoiceRecognitionService && !isInitialized) {
                        initializeVoskSafely()
                    }
                    // Esperar e iniciar escucha
                    waitForVoskAndStartListening()
                }
            }
            ACTION_STOP_LISTENING -> {
                stopListening()
                serviceScope.launch {
                    preferences.setVoiceServiceEnabled(false)
                }
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
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "SafeVoice::VoiceRecognitionWakeLock"
        )
        // Mantener el servicio activo indefinidamente - ESENCIAL para operaciones policiales
        wakeLock?.acquire()
        Log.d("VoiceService", "🔒 WakeLock POLICIAL adquirido - Monitoreo 24/7 activado")
    }

    private suspend fun initializeVoskSafely() {
        // Proteger contra múltiples inicializaciones simultáneas
        synchronized(this@VoiceRecognitionService) {
            if (isInitializing || isInitialized) {
                Log.d("VoiceService", "⚠️ Vosk ya está inicializándose o inicializado")
                return
            }
            isInitializing = true
        }
        
        var attempts = 0
        val maxAttempts = 3 // Reducir intentos para evitar crashes
        
        while (attempts < maxAttempts && !isInitialized) {
            attempts++
            Log.d("VoiceService", "Intento de inicialización de Vosk #$attempts")
            
            try {
                initializeVosk()
                if (model != null && recognizer != null) {
                    synchronized(this@VoiceRecognitionService) {
                        isInitialized = true
                        isInitializing = false
                    }
                    Log.d("VoiceService", "✅ Vosk inicializado exitosamente en intento #$attempts")
                    return
                }
            } catch (e: Exception) {
                Log.w("VoiceService", "Intento #$attempts falló: ${e.message}")
            }
            
            // Esperar antes del siguiente intento
            kotlinx.coroutines.delay(2000L)
        }
        
        synchronized(this@VoiceRecognitionService) {
            isInitializing = false
            if (!isInitialized) {
                Log.e("VoiceService", "❌ No se pudo inicializar Vosk después de $maxAttempts intentos")
            }
        }
    }
    
    private suspend fun waitForVoskAndStartListening() {
        Log.d("VoiceService", "🔄 Esperando a que Vosk esté listo...")
        
        // Esperar hasta que Vosk esté inicializado (máximo 10 segundos)
        var waitTime = 0
        val maxWaitTime = 10000 // 10 segundos
        val checkInterval = 500L // 0.5 segundos
        
        while (!isInitialized && waitTime < maxWaitTime) {
            kotlinx.coroutines.delay(checkInterval)
            waitTime += checkInterval.toInt()
            
            if (waitTime % 2000 == 0) { // Log cada 2 segundos
                Log.d("VoiceService", "⏳ Esperando Vosk... ${waitTime/1000}s")
            }
        }
        
        if (isInitialized && model != null && recognizer != null) {
            Log.d("VoiceService", "🎉 Vosk está listo, iniciando escucha...")
            startListening()
        } else {
            Log.w("VoiceService", "⚠️ Vosk no está listo después de ${maxWaitTime/1000}s, saltando inicialización de escucha")
        }
    }

    private fun initializeVosk() {
        try {
            Log.d("VoiceService", "Iniciando inicialización de Vosk...")
            LibVosk.setLogLevel(LogLevel.WARNINGS)

            val modelDir = File(filesDir, "vosk-models/spanish")
            Log.d("VoiceService", "Buscando modelo en: ${modelDir.absolutePath}")
            Log.d("VoiceService", "El directorio existe: ${modelDir.exists()}")
            
            if (modelDir.exists() && hasRequiredModelFiles(modelDir)) {
                Log.d("VoiceService", "Creando modelo Vosk...")
                
                // Verificar que no haya instancias previas
                model?.close()
                recognizer?.close()
                
                model = Model(modelDir.absolutePath)
                recognizer = Recognizer(model, 16000.0f)
                Log.d("VoiceService", "✅ Vosk inicializado correctamente")
            } else {
                Log.e("VoiceService", "❌ Directorio del modelo no existe o archivos faltantes")
                // Forzar extracción del modelo desde la aplicación principal
                triggerModelExtraction()
                throw Exception("Modelo no disponible, se está extrayendo...")
            }

        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error inicializando Vosk", e)
            throw e
        }
    }
    
    private fun hasRequiredModelFiles(modelDir: File): Boolean {
        val requiredFiles = listOf(
            "am/final.mdl",
            "conf/mfcc.conf", 
            "conf/model.conf",
            "graph/Gr.fst",
            "graph/HCLr.fst"
        )
        
        for (file in requiredFiles) {
            if (!File(modelDir, file).exists()) {
                Log.w("VoiceService", "Archivo faltante: $file")
                return false
            }
        }
        return true
    }
    
    private fun triggerModelExtraction() {
        Log.d("VoiceService", "🔄 Solicitando extracción forzada del modelo...")
        // Forzar extracción directa
        MyApplication.forceModelExtraction()
        // También enviar broadcast como respaldo
        val intent = Intent("com.example.demoappchat.EXTRACT_VOSK_MODEL")
        sendBroadcast(intent)
    }
    

    private fun startListening() {
        synchronized(this) {
            Log.d("VoiceService", "Intentando iniciar escucha...")
            
            if (isListening) {
                Log.w("VoiceService", "⚠️ Ya está escuchando, ignorando nueva solicitud")
                return
            }
            
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("VoiceService", "❌ Sin permiso de RECORD_AUDIO")
                return
            }
            
            if (!isInitialized || model == null || recognizer == null) {
                Log.e("VoiceService", "❌ Modelo o reconocedor de Vosk no inicializados")
                return
            }
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        try {
            audioRecord?.startRecording()
            val state = audioRecord?.recordingState
            Log.d("VoiceService", "Estado de AudioRecord: $state")
            isListening = true
        } catch (e: Exception) {
            Log.e("VoiceService", "Error iniciando AudioRecord", e)
            return
        }

        Thread {
            val buffer = ShortArray(bufferSize)
            Log.d("VoiceService", "Iniciando thread de escucha")
            while (isListening && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        recognizer?.acceptWaveForm(buffer, bytesRead)?.let { isEndOfSpeech ->
                            if (isEndOfSpeech) {
                                handleSpeechResult()
                            }
                        }
                    }
                    Thread.sleep(100) // Pequeña pausa para no saturar CPU
                } catch (e: Exception) {
                    Log.e("VoiceService", "Error en loop de audio", e)
                    break
                }
            }
            Log.d("VoiceService", "Thread de escucha terminado")
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

                serviceScope.launch {
                    // Check if user is currently in a chat (CRITICAL: Only work within chat groups)
                    val currentChatId = preferences.getCurrentChatId().first()
                    if (currentChatId.isNullOrEmpty()) {
                        Log.d("VoiceService", "No active chat - voice commands disabled")
                        return@launch
                    }

                    // Verificar comandos de activación configurados
                    val activationCommands = preferences.getVoiceCommands().first()
                    if (activationCommands.any { command -> text.contains(command) }) {
                        triggerEmergencyAction(text, currentChatId)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerEmergencyAction(recognizedText: String, chatId: String) {
        Log.d("VoiceService", "Comando detectado: $recognizedText en chat: $chatId")

        // Determinar tipo de grabación según el comando
        val recordingType = when {
            recognizedText.contains("grabar audio") || recognizedText.contains("audio") || recognizedText.contains("óyeme") -> RecordingType.AUDIO
            recognizedText.contains("grabar video") || recognizedText.contains("video") || recognizedText.contains("cámara") -> RecordingType.VIDEO
            else -> RecordingType.AUDIO // Por defecto grabar audio
        }

        // Iniciar grabación para el chat específico
        startRecording(recordingType, chatId)

        serviceScope.launch {
            val discreteMode = preferences.getDiscreteMode().first()
            if (discreteMode) {
                // Modo discreto: sin notificaciones visibles
                sendSilentEmergencyAlert(recognizedText, recordingType)
            } else {
                // Modo normal: con notificaciones
                sendEmergencyAlert(recognizedText, recordingType)
            }
        }
    }

    private fun startRecording(type: RecordingType, chatId: String? = null) {
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
                val chatId = preferences.getCurrentChatId().first() ?: return@launch
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
                // Send emergency alert with current transmission radius
                val radius = preferences.getTransmissionRadius().first() * 1000 // Convert km to meters
                repository.sendEmergencyAlert(command, radius.toInt(), type.name)
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
            else -> "Comandos de voz activados"
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
        Log.d("VoiceService", "🛑 Servicio destruido")
        
        synchronized(VoiceRecognitionService::class.java) {
            if (serviceInstance == this) {
                isServiceRunning = false
                serviceInstance = null
            }
        }
        
        try {
            stopListening()
            stopRecording()
            recognizer?.close()
            model?.close()
            wakeLock?.release()
        } catch (e: Exception) {
            Log.e("VoiceService", "Error cleaning up service", e)
        }
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
        private const val VOSK_READY_NOTIFICATION_ID = 1003
        
        // Control de instancia única
        @Volatile
        private var isServiceRunning = false
        
        @Volatile
        private var serviceInstance: VoiceRecognitionService? = null
    }
}