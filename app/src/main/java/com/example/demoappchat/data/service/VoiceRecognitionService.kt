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
import com.google.firebase.messaging.FirebaseMessaging
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
    
    @Inject
    lateinit var androidSpeechService: AndroidSpeechService
    
    @Inject 
    lateinit var audioPatternDetector: AudioPatternDetector

    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null
    private var isListening = false
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Variables para sistema híbrido de detección
    private var currentDetectionMode = DetectionMode.NONE
    private var speechServiceRunning = false
    
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
    
    // FCM Token Management
    private var fcmTokenRegistered = false

    enum class RecordingType {
        NONE, AUDIO, VIDEO
    }
    
    enum class DetectionMode {
        NONE, ANDROID_SPEECH, VOSK, AUDIO_PATTERNS
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
        initializeFCMIntegration()
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

    /**
     * INTEGRACIÓN FCM PARA OPERACIONES 24/7
     * Registra el dispositivo para recibir notificaciones de llamadas grupales
     */
    private fun initializeFCMIntegration() {
        serviceScope.launch {
            try {
                // Obtener token FCM
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("VoiceService", "⚠️ Error obteniendo token FCM", task.exception)
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    Log.d("VoiceService", "🔑 Token FCM obtenido: ${token.take(20)}...")
                    
                    // Registrar token en Firebase Database para que otros usuarios puedan enviar notificaciones
                    registerFCMToken(token)
                }
                
                // Suscribirse a tópicos relevantes para operaciones policiales
                subscribeFCMTopics()
                
            } catch (e: Exception) {
                Log.e("VoiceService", "❌ Error inicializando FCM", e)
            }
        }
    }

    private fun registerFCMToken(token: String) {
        serviceScope.launch {
            try {
                // Registrar token en el repositorio para que otros usuarios puedan notificar
                repository.registerFCMToken(token)
                fcmTokenRegistered = true
                Log.d("VoiceService", "✅ Token FCM registrado en Firebase")
                
            } catch (e: Exception) {
                Log.e("VoiceService", "❌ Error registrando token FCM", e)
            }
        }
    }

    private fun subscribeFCMTopics() {
        // Suscribirse a tópicos de emergencia y alertas grupales
        val topics = listOf(
            "emergency_alerts",
            "group_calls", 
            "voice_commands",
            "police_operations"
        )
        
        topics.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("VoiceService", "✅ Suscrito a tópico: $topic")
                    } else {
                        Log.w("VoiceService", "⚠️ Error suscribiéndose a $topic", task.exception)
                    }
                }
        }
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
        synchronized(this@VoiceRecognitionService) {
            if (isInitializing || isInitialized) {
                Log.d("VoiceService", "⚠️ Sistema de voz ya está inicializándose o inicializado")
                return
            }
            isInitializing = true
        }
        
        try {
            // OPCIÓN 1: Android SpeechRecognizer (PRINCIPAL - MÁS CONFIABLE)
            if (initializeAndroidSpeechService()) {
                currentDetectionMode = DetectionMode.ANDROID_SPEECH
                synchronized(this@VoiceRecognitionService) {
                    isInitialized = true
                    isInitializing = false
                }
                Log.d("VoiceService", "✅ Android SpeechRecognizer inicializado - MODO PRINCIPAL")
                return
            }
            
            // OPCIÓN 2: Detector de patrones de audio (MÁS CONFIABLE QUE VOSK)
            if (initializeAudioPatternDetector()) {
                currentDetectionMode = DetectionMode.AUDIO_PATTERNS
                synchronized(this@VoiceRecognitionService) {
                    isInitialized = true
                    isInitializing = false
                }
                Log.d("VoiceService", "✅ Detector de patrones activado - MODO FALLBACK")
                return
            }
            
            // OPCIÓN 3: Vosk como último recurso
            Log.d("VoiceService", "📱 Intentando Vosk como último recurso...")
            try {
                initializeVosk()
                if (model != null && recognizer != null) {
                    currentDetectionMode = DetectionMode.VOSK
                    synchronized(this@VoiceRecognitionService) {
                        isInitialized = true
                        isInitializing = false
                    }
                    Log.d("VoiceService", "✅ Vosk inicializado - MODO EMERGENCIA")
                    return
                }
            } catch (e: Exception) {
                Log.w("VoiceService", "Vosk falló: ${e.message}")
            }
            
            // Si todo falla, al menos activar modo básico
            currentDetectionMode = DetectionMode.AUDIO_PATTERNS
            synchronized(this@VoiceRecognitionService) {
                isInitialized = true
                isInitializing = false
            }
            Log.d("VoiceService", "⚠️ Sistema básico activado")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error crítico en inicialización", e)
            synchronized(this@VoiceRecognitionService) {
                isInitializing = false
            }
        }
    }
    
    private fun initializeAndroidSpeechService(): Boolean {
        return try {
            if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
                Log.d("VoiceService", "🎤 Inicializando Android SpeechRecognizer...")
                
                // Usar el servicio inyectado con callback
                androidSpeechService.startListening { recognizedText ->
                    Log.d("VoiceService", "🎯 AndroidSpeech detectó: '$recognizedText'")
                    serviceScope.launch {
                        handleVoiceCommand(recognizedText)
                    }
                }
                speechServiceRunning = true
                Log.d("VoiceService", "✅ Android SpeechRecognizer funcionando")
                true
            } else {
                Log.d("VoiceService", "❌ Android SpeechRecognizer no disponible")
                false
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error inicializando Android Speech", e)
            false
        }
    }
    
    private fun initializeAudioPatternDetector(): Boolean {
        return try {
            Log.d("VoiceService", "🎵 Inicializando detector de patrones...")
            
            audioPatternDetector.startDetection { pattern ->
                Log.d("VoiceService", "🎯 Patrón detectado: '$pattern'")
                serviceScope.launch {
                    // Convertir patrón a comando de voz
                    val voiceCommand = convertPatternToCommand(pattern)
                    handleVoiceCommand(voiceCommand)
                }
            }
            
            Log.d("VoiceService", "✅ Detector de patrones funcionando")
            true
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error inicializando detector de patrones", e)
            false
        }
    }
    
    private fun convertPatternToCommand(pattern: String): String {
        return when (pattern) {
            "tap_tap_tap" -> "ayuda"
            "long_short_long" -> "emergencia"
            "whistle_pattern" -> "alerta"
            "clap_sequence" -> "grabar"
            else -> "comando"
        }
    }
    
    private fun initializeAudioPatterns() {
        try {
            Log.d("VoiceService", "🎵 Inicializando detector de patrones de audio...")
            // El detector de patrones siempre funciona como fallback
            Log.d("VoiceService", "✅ Detector de patrones listo")
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error en detector de patrones", e)
        }
    }
    
    private suspend fun waitForVoskAndStartListening() {
        Log.d("VoiceService", "🔄 Esperando a que el sistema de detección esté listo...")
        
        // Esperar hasta que algún sistema esté inicializado (máximo 10 segundos)
        var waitTime = 0
        val maxWaitTime = 10000 // 10 segundos
        val checkInterval = 500L // 0.5 segundos
        
        while (!isInitialized && waitTime < maxWaitTime) {
            kotlinx.coroutines.delay(checkInterval)
            waitTime += checkInterval.toInt()
            
            if (waitTime % 2000 == 0) { // Log cada 2 segundos
                Log.d("VoiceService", "⏳ Esperando sistema de detección... ${waitTime/1000}s")
            }
        }
        
        if (isInitialized) {
            when (currentDetectionMode) {
                DetectionMode.ANDROID_SPEECH -> {
                    Log.d("VoiceService", "🎉 Android SpeechRecognizer listo y funcionando")
                    // AndroidSpeech ya está escuchando automáticamente
                }
                DetectionMode.AUDIO_PATTERNS -> {
                    Log.d("VoiceService", "🎉 Detector de patrones listo y funcionando")
                    // AudioPatternDetector ya está detectando automáticamente
                }
                DetectionMode.VOSK -> {
                    if (model != null && recognizer != null) {
                        Log.d("VoiceService", "🎉 Vosk listo, iniciando escucha...")
                        startListening()
                    }
                }
                DetectionMode.NONE -> {
                    Log.w("VoiceService", "⚠️ Ningún sistema de detección disponible")
                }
            }
        } else {
            Log.w("VoiceService", "⚠️ Sistema no listo después de ${maxWaitTime/1000}s")
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
                    handleVoiceCommand(text)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * FUNCIÓN UNIFICADA: Maneja comandos de voz desde cualquier sistema de detección
     */
    private suspend fun handleVoiceCommand(recognizedText: String) {
        try {
            val text = recognizedText.lowercase().trim()
            Log.d("VoiceService", "🎯 Procesando comando: '$text'")
            
            // Check if user is currently in a chat (CRITICAL: Only work within chat groups)
            val currentChatId = preferences.getCurrentChatId().first()
            if (currentChatId.isNullOrEmpty()) {
                Log.d("VoiceService", "⚠️ Sin chat activo - comandos de voz deshabilitados")
                return
            }

            // Verificar comandos de activación configurados
            val activationCommands = preferences.getVoiceCommands().first()
            val matchedCommand = activationCommands.find { command -> 
                text.contains(command.lowercase()) 
            }
            
            if (matchedCommand != null) {
                Log.d("VoiceService", "🚨 COMANDO POLICIAL DETECTADO: '$matchedCommand' - Iniciando acción inmediata")
                
                // GRABACIÓN Y TRANSMISIÓN AUTOMÁTICA
                startImmediateRecordingAndTransmit(text, currentChatId, matchedCommand)
            } else {
                Log.d("VoiceService", "🔍 Comando no reconocido: '$text'")
                
                // Log para debugging - mostrar comandos disponibles
                Log.d("VoiceService", "📋 Comandos configurados: ${activationCommands.joinToString()}")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error procesando comando de voz", e)
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Grabación inmediata cuando se detecta comando
     */
    private suspend fun startImmediateRecordingAndTransmit(recognizedText: String, chatId: String, command: String) {
        try {
            Log.d("VoiceService", "🎙️ GRABACIÓN POLICIAL INMEDIATA - Comando: '$command'")
            
            // 1. Determinar tipo de grabación según el comando
            val recordingType = when {
                command.contains("video") || recognizedText.contains("video") || recognizedText.contains("cámara") -> RecordingType.VIDEO
                else -> RecordingType.AUDIO // Por defecto grabar audio
            }
            
            // 2. Detener cualquier grabación previa
            if (isRecording) {
                stopRecording()
                kotlinx.coroutines.delay(200) // Pausa breve
            }
            
            // 3. Notificar inicio de grabación
            sendRecordingStartNotification(command, recordingType, chatId)
            
            // 4. Iniciar grabación inmediata
            startRecording(recordingType, chatId)
            
            // 5. Grabar por tiempo específico (configurable desde UserPreferences)
            val userDuration = preferences.getAutoRecordingDuration().first() * 1000L // Convertir a ms
            val recordingDuration = when (recordingType) {
                RecordingType.VIDEO -> 15000L // 15 segundos para video (fijo)
                RecordingType.AUDIO -> userDuration // Duración configurable para audio
                RecordingType.NONE -> 0L
            }
            
            Log.d("VoiceService", "⏱️ Grabando por ${recordingDuration/1000} segundos...")
            
            // 6. Esperar tiempo de grabación
            kotlinx.coroutines.delay(recordingDuration)
            
            // 6. Detener grabación y enviar automáticamente
            stopRecording()
            
            // 7. Enviar notificación de comando ejecutado
            sendCommandExecutedNotification(command, recordingType, chatId)
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error en grabación inmediata", e)
        }
    }
    
    private suspend fun sendCommandExecutedNotification(command: String, type: RecordingType, chatId: String) {
        try {
            // Crear mensaje de sistema informando del comando
            val systemMessage = com.example.demoappchat.data.model.ChatMessage(
                chatId = chatId,
                userId = "SYSTEM_POLICE", // ID especial para mensajes del sistema policial
                userName = "🚔 Sistema Policial",
                content = "🚨 COMANDO EJECUTADO: '$command'\n📼 ${type.name.lowercase()} transmitido automáticamente",
                messageType = com.example.demoappchat.data.model.MessageType.SYSTEM,
                timestamp = System.currentTimeMillis()
            )
            
            // Enviar mensaje al chat
            repository.sendMessage(systemMessage)
            
            Log.d("VoiceService", "✅ Notificación de comando enviada al chat: $chatId")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error enviando notificación de comando", e)
        }
    }
    
    private suspend fun sendRecordingStartNotification(command: String, type: RecordingType, chatId: String) {
        try {
            // Obtener duración configurada
            val userDuration = preferences.getAutoRecordingDuration().first()
            val duration = if (type == RecordingType.VIDEO) 15 else userDuration
            
            // Crear mensaje inmediato de inicio de grabación
            val startMessage = com.example.demoappchat.data.model.ChatMessage(
                chatId = chatId,
                userId = "SYSTEM_POLICE",
                userName = "🚔 Sistema Policial",
                content = "🔴 GRABANDO AHORA...\n🎤 Comando: '$command'\n⏱️ Duración: $duration segundos",
                messageType = com.example.demoappchat.data.model.MessageType.SYSTEM,
                timestamp = System.currentTimeMillis()
            )
            
            repository.sendMessage(startMessage)
            Log.d("VoiceService", "🔴 Notificación de INICIO de grabación enviada")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error enviando notificación de inicio", e)
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
            // Limpiar todos los sistemas de detección
            when (currentDetectionMode) {
                DetectionMode.ANDROID_SPEECH -> {
                    androidSpeechService.stopListening()
                    speechServiceRunning = false
                }
                DetectionMode.AUDIO_PATTERNS -> {
                    audioPatternDetector.stopDetection()
                }
                DetectionMode.VOSK -> {
                    stopListening()
                    recognizer?.close()
                    model?.close()
                }
                DetectionMode.NONE -> {}
            }
            
            stopRecording()
            wakeLock?.release()
            
            Log.d("VoiceService", "✅ Limpieza completa del servicio")
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error limpiando servicio", e)
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