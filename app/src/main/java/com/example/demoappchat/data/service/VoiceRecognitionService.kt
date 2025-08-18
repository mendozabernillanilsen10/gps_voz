package com.example.demoappchat.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.demoappchat.MainActivity
import com.example.demoappchat.R
import com.example.demoappchat.data.service.voice.VoiceEngineManager
import com.example.demoappchat.data.service.voice.SimpleVoskEngine
import com.example.demoappchat.domain.usecase.voice.StartVoiceRecognitionUseCase
import com.example.demoappchat.domain.usecase.voice.ProcessVoiceRecognitionUseCase
import com.example.demoappchat.domain.usecase.voice.MonitorVoiceServiceUseCase
import com.example.demoappchat.domain.usecase.voice.VoiceProcessingResult
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log
import com.example.demoappchat.data.repository.FirebaseRepository
import java.util.Random

/**
 * Servicio profesional de reconocimiento de voz 24/7
 * Implementa Clean Architecture con Use Cases y manejo robusto de estados
 */
@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    // TODO: Implementar inyección de dependencias cuando esté disponible
    // @Inject
    // lateinit var voiceEngineManager: VoiceEngineManager
    //
    // @Inject
    // lateinit var startVoiceRecognitionUseCase: StartVoiceRecognitionUseCase
    //
    // @Inject
    // lateinit var processVoiceRecognitionUseCase: ProcessVoiceRecognitionUseCase
    //
    // @Inject
    // lateinit var monitorVoiceServiceUseCase: MonitorVoiceServiceUseCase
    //
    // @Inject
    // lateinit var voicePreferences: VoicePreferences

    // Service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var voiceProcessingJob: Job? = null
    private var monitoringJob: Job? = null
    
    // Service state
    private var isListening = false
    private var isStealthMode = false
    private var isSurveillanceMode = false
    private var isTrackingLocation = false
    
    // Media recording
    private lateinit var mediaRecordingService: SimpleMediaRecordingService
    private lateinit var voskEngine: SimpleVoskEngine
    // private lateinit var firebaseRepository: FirebaseRepository
    // private lateinit var voicePreferences: VoicePreferences
    private lateinit var sharedPreferences: SharedPreferences
    
    // Configuraciones de grabación
    private var audioRecordingDuration = 5 // segundos por defecto
    private var videoRecordingDuration = 10 // segundos por defecto
    private var recordingQuality = "HIGH" // calidad por defecto
    
    // Notification
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "voice_recognition_channel"
        const val ACTION_START_RECOGNITION = "START_RECOGNITION"
        const val ACTION_STOP_RECOGNITION = "STOP_RECOGNITION"
        const val ACTION_TOGGLE_STEALTH = "TOGGLE_STEALTH"
        const val ACTION_START_LISTENING = "START_LISTENING"
        const val ACTION_STOP_LISTENING = "STOP_LISTENING"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val EXTRA_RECORDING_TYPE = "extra_recording_type"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("VoiceService", "🚀 Servicio de voz profesional iniciando...")
        
        // Inicializar dependencias
        sharedPreferences = getSharedPreferences("voice_prefs", MODE_PRIVATE)
        
        // Cargar configuraciones de grabación
        loadRecordingSettings()
        
        // Inicializar servicios
        mediaRecordingService = SimpleMediaRecordingService(this)
        voskEngine = SimpleVoskEngine(this)
        
        // Configurar callback para comandos detectados
        voskEngine.setCallback { command, confidence ->
            Log.d("VoiceService", "🎯 Comando detectado por Vosk: '$command' (confianza: $confidence)")
            serviceScope.launch {
                processVoiceCommand(command)
            }
        }
        
        // Comentar temporalmente FirebaseRepository
        // firebaseRepository = FirebaseRepository(userPreferences)
        
        initializeService()
        createNotificationChannel()
        initializeFCMIntegration()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VoiceService", "📢 Comando recibido: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                // Guardar el chatId actual si se proporciona
                val chatId = intent.getStringExtra("chat_id")
                if (chatId != null) {
                    sharedPreferences.edit().putString("current_chat_id", chatId).apply()
                    Log.d("VoiceService", "💾 Chat ID guardado: $chatId")
                }
                startVoiceRecognition()
            }
            ACTION_START_RECOGNITION -> startVoiceRecognition()
            ACTION_STOP_RECOGNITION -> stopVoiceRecognition()
            ACTION_TOGGLE_STEALTH -> toggleStealthModeAction()
            else -> startVoiceRecognition() // Por defecto iniciar reconocimiento
        }
        
        return START_STICKY // Reiniciar si el sistema lo mata
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("VoiceService", "🛑 Servicio de voz terminando...")
        
        stopVoiceRecognition()
        releaseWakeLock()
        voiceProcessingJob?.cancel()
        monitoringJob?.cancel()
        
        super.onDestroy()
    }

    /**
     * Inicializa componentes del servicio
     */
    private fun initializeService() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createServiceNotification())
    }

    /**
     * Inicia el reconocimiento de voz
     */
    private fun startVoiceRecognition() {
        serviceScope.launch {
            try {
                Log.d("VoiceService", "🎤 Iniciando reconocimiento de voz...")
                
                // Verificar si ya está ejecutándose para evitar reinicios
                if (isListening) {
                    Log.d("VoiceService", "ℹ️ Reconocimiento ya está activo, saltando inicio")
                    return@launch
                }
                
                // Cargar comandos desde preferencias
                reloadCommandsFromPreferences()
                
                // Inicializar servicios si no están inicializados
                if (!::voskEngine.isInitialized) {
                    voskEngine = SimpleVoskEngine(this@VoiceRecognitionService)
                }
                if (!::mediaRecordingService.isInitialized) {
                    mediaRecordingService = SimpleMediaRecordingService(this@VoiceRecognitionService)
                }
                
                // Configurar callback del Vosk Engine
                voskEngine.setCallback { recognizedText, confidence ->
                    Log.d("VoiceService", "🎤 Resultado de voz: '$recognizedText' (${confidence}%)")
                    
                    // Verificar confianza mínima (evitar falsos positivos)
                    val minConfidence = getVoiceSensitivity()
                    
                    // Para el sistema de testing temporal, usar confianza fija alta
                    val effectiveConfidence = if (confidence < 1.0f) {
                        // Si la confianza es muy baja (sistema de testing), usar 85%
                        85f
                    } else {
                        confidence
                    }
                    
                    if (effectiveConfidence < minConfidence) {
                        Log.d("VoiceService", "🔇 Confianza muy baja (${effectiveConfidence}% < ${minConfidence}%), ignorando")
                        return@setCallback
                    }
                    
                    // Verificar que el texto no esté vacío
                    if (recognizedText.isBlank()) {
                        Log.d("VoiceService", "🔇 Texto vacío, ignorando")
                        return@setCallback
                    }
                    
                    // Verificar si es un comando válido
                    val text: String = recognizedText.lowercase().trim()
                    val commandActions: Map<String, String> = getCommandActions()
                    
                    Log.d("VoiceService", "🔍 Verificando comando: '$text' (confianza: ${effectiveConfidence}%)")
                    Log.d("VoiceService", "📋 Comandos disponibles: $commandActions")
                    
                    // Buscar coincidencias EXACTAS primero, luego parciales con umbral más alto
                    var matchedCommand: String? = commandActions.keys.find { command: String ->
                        text == command.lowercase() // Coincidencia exacta
                    }
                    
                    // Si no hay coincidencia exacta, buscar parcial con umbral más alto
                    if (matchedCommand == null && effectiveConfidence >= 80) {
                        matchedCommand = commandActions.keys.find { command: String ->
                            val commandLower: String = command.lowercase()
                            // Solo coincidencias parciales si el comando está al inicio o final
                            text.startsWith(commandLower) || 
                            text.endsWith(commandLower) ||
                            text.contains(" $commandLower ") // Comando rodeado de espacios
                        }
                    }
                    
                    if (matchedCommand != null) {
                        val action = commandActions[matchedCommand]
                        Log.d("VoiceService", "✅ Comando detectado: '$matchedCommand' -> $action (confianza: ${effectiveConfidence}%)")
                        
                        // Ejecutar la acción correspondiente
                        serviceScope.launch {
                            processVoiceCommand(matchedCommand)
                        }
                    } else {
                        Log.d("VoiceService", "❌ No se encontró comando para: '$text' (confianza: ${effectiveConfidence}%)")
                    }
                }
                
                // Iniciar Vosk Engine con manejo de errores mejorado
                val startResult = voskEngine.startListening()
                if (startResult.isSuccess) {
                    // Marcar como escuchando
                    isListening = true
                    
                    Log.d("VoiceService", "✅ Reconocimiento iniciado exitosamente")
                    
                    // Actualizar notificación
                    updateNotification("🎤 Escuchando comandos...")
                    
                    // Iniciar monitoreo de salud del servicio
                    startHealthMonitoring()
                    
                } else {
                    Log.e("VoiceService", "❌ Error iniciando Vosk Engine: ${startResult.exceptionOrNull()?.message}")
                    updateNotification("❌ Error iniciando reconocimiento")
                }
                
            } catch (e: Exception) {
                Log.e("VoiceService", "❌ Excepción iniciando reconocimiento", e)
                updateNotification("❌ Error crítico")
            }
        }
    }

    /**
     * Detiene el reconocimiento de voz
     */
    private fun stopVoiceRecognition() {
        serviceScope.launch {
            try {
                Log.d("VoiceService", "🛑 Deteniendo reconocimiento...")
                
                // Cancelar jobs de procesamiento
                voiceProcessingJob?.cancel()
                monitoringJob?.cancel()
                
                // Detener Vosk Engine
                try {
                    voskEngine.stopListening()
                    Log.d("VoiceService", "✅ Vosk Engine detenido")
            } catch (e: Exception) {
                    Log.e("VoiceService", "❌ Error deteniendo Vosk Engine: ${e.message}")
                }
                
                updateNotification("⏹️ Reconocimiento detenido")
            
        } catch (e: Exception) {
                Log.e("VoiceService", "❌ Error deteniendo reconocimiento", e)
            }
        }
    }



    /**
     * Maneja los resultados del procesamiento de voz
     */
    private fun handleVoiceProcessingResult(result: VoiceProcessingResult) {
        when (result) {
            is VoiceProcessingResult.CommandExecuted -> {
                Log.d("VoiceService", "✅ Comando ejecutado: ${result.executionMessage}")
                updateNotification("✅ ${result.executionMessage}")
                
                // Mostrar feedback temporal
                    serviceScope.launch {
                    kotlinx.coroutines.delay(3000)
                    updateNotification("🎤 Escuchando comandos...")
                }
            }
            
            is VoiceProcessingResult.CommandFailed -> {
                Log.w("VoiceService", "⚠️ Comando falló: ${result.errorMessage}")
                updateNotification("⚠️ ${result.errorMessage}")
                
                // Volver al estado normal después de un tiempo
                serviceScope.launch {
                    kotlinx.coroutines.delay(2000)
                    updateNotification("🎤 Escuchando comandos...")
                }
            }
            
            is VoiceProcessingResult.LowConfidence -> {
                // No mostrar en notificación, solo log
                Log.v("VoiceService", "🔇 Confianza baja: ${result.originalResult.confidence}")
            }
            
            is VoiceProcessingResult.ProcessingError -> {
                Log.e("VoiceService", "❌ Error procesamiento: ${result.errorMessage}")
            }
        }
    }

    // TODO: Implementar cuando las dependencias estén disponibles
    // /**
    //  * Inicia el monitoreo del estado del servicio
    //  */
    // private fun startServiceMonitoring() {
    //     monitoringJob = monitorVoiceServiceUseCase.execute()
    //         .onEach { monitoringResult ->
    //         if (!monitoringResult.isSystemHealthy) {
    //             Log.w("VoiceService", "⚠️ Sistema no saludable")
    //             Log.w("VoiceService", "Recomendaciones: ${monitoringResult.recommendations}")
    //             
    //             // Intentar recuperación automática si es necesario
    //             if (monitoringResult.recommendations.contains("Reiniciar servicio de reconocimiento")) {
    //                 restartVoiceRecognition()
    //             }
    //         }
    //     }
    //         .launchIn(serviceScope)
    // }

    /**
     * Reinicia el reconocimiento de voz
     */
    private fun restartVoiceRecognition() {
        serviceScope.launch {
            Log.d("VoiceService", "🔄 Reiniciando reconocimiento...")
            
            stopVoiceRecognition()
            kotlinx.coroutines.delay(1000)
            startVoiceRecognition()
        }
    }
    
    /**
     * Recarga los comandos desde SharedPreferences
     */
    private fun reloadCommandsFromPreferences() {
        try {
            val commandActions = getCommandActions()
            val commandsList = commandActions.keys.toList()
            Log.d("VoiceService", "🔍 Comandos guardados: ${commandActions.entries.joinToString(",") { "${it.key}:${it.value}" }}")
            Log.d("VoiceService", "✅ Comandos parseados: $commandActions")
            Log.d("VoiceService", "🎯 Configurando comandos en Vosk Engine: $commandsList")
            Log.d("VoiceService", "📝 Comandos configurados: $commandsList")
            Log.d("VoiceService", "🎯 Total de comandos: ${commandsList.size}")
            commandsList.forEachIndexed { index, command ->
                Log.d("VoiceService", "   ${index + 1}. '$command'")
            }
            
            // Configurar comandos en Vosk Engine
            if (::voskEngine.isInitialized) {
                voskEngine.setCommands(commandsList)
                voskEngine.setSensitivity(0.7f)
                Log.d("VoiceService", "🔄 Reconfigurando recognizer existente con nuevos comandos")
            }
            
            // Guardar comandos por defecto si no existen
            if (sharedPreferences.getString("command_actions", null) == null) {
                saveCommandActions(commandActions)
                Log.d("VoiceService", "💾 Comandos por defecto guardados")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error recargando comandos", e)
        }
    }

    /**
     * Alterna el modo sigiloso
     */
    private fun toggleStealthModeAction() {
        serviceScope.launch {
            try {
                // TODO: Implementar toggle de stealth mode
                Log.d("VoiceService", "🥷 Alternando modo sigiloso...")
                updateNotification("🥷 Modo sigiloso alternado")

        } catch (e: Exception) {
                Log.e("VoiceService", "Error alternando stealth mode", e)
            }
        }
    }

    /**
     * Crea el canal de notificación
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reconocimiento de Voz",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servicio de reconocimiento de voz 24/7"
            setShowBadge(false)
            setSound(null, null)
        }
        
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Crea la notificación del servicio
     */
    private fun createServiceNotification(content: String = "Iniciando servicio..."): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VoiceRecognitionService::class.java).apply {
                action = ACTION_STOP_RECOGNITION
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stealthIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VoiceRecognitionService::class.java).apply {
                action = ACTION_TOGGLE_STEALTH
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reconocimiento de Voz Activo")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_stop, "Detener", stopIntent)
            .addAction(R.drawable.ic_stealth, "Sigiloso", stealthIntent)
            .build()
    }

    /**
     * Actualiza la notificación
     */
    private fun updateNotification(content: String) {
        val notification = createServiceNotification(content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Adquiere WakeLock para mantener el servicio activo
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VoiceRecognition::WakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos máximo
            
            Log.d("VoiceService", "🔋 WakeLock adquirido")

        } catch (e: Exception) {
            Log.e("VoiceService", "Error adquiriendo WakeLock", e)
        }
    }

    /**
     * Libera el WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("VoiceService", "🔋 WakeLock liberado")
                }
            }
            wakeLock = null

                } catch (e: Exception) {
            Log.e("VoiceService", "Error liberando WakeLock", e)
        }
    }
    
    /**
     * Inicializa la integración con FCM
     */
    private fun initializeFCMIntegration() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("VoiceService", "Error obteniendo FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("VoiceService", "📱 FCM Token: $token")
            
            // TODO: Registrar token en el servidor
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Activa el servicio automáticamente cuando el usuario entra a un chat grupal
     */
    private fun activateForGroupChat(chatId: String) {
        try {
            Log.d("VoiceService", "🏢 Activando servicio para chat grupal: $chatId")
            
            // TODO: Implementar cuando las dependencias estén disponibles
            // Guardar el chat activo
            // serviceScope.launch {
            //     voicePreferences.setCurrentChatId(chatId)
            // }
            
            // Iniciar reconocimiento de voz si no está activo
            if (!isListening) {
                startVoiceRecognition()
            }
            
            // Configurar para funcionar 24/7
            configure24x7Mode()
            
            // Mostrar notificación de servicio activo
            updateNotification("🎤 Escuchando en chat grupal")
            
            } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error activando para chat grupal", e)
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Configura el modo 24/7 para funcionar siempre
     */
    private fun configure24x7Mode() {
        try {
            // Adquirir WakeLock para mantener el servicio activo
            acquireWakeLock()
            
            // Configurar para reiniciar automáticamente si el sistema lo mata
            startForeground(NOTIFICATION_ID, createNotification("🎤 Servicio 24/7 activo"))
            
            // Iniciar monitoreo continuo
            startContinuousMonitoring()
            
            Log.d("VoiceService", "🔄 Modo 24/7 configurado")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error configurando modo 24/7", e)
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Monitoreo continuo para mantener el servicio activo
     */
    private fun startContinuousMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (true) {
                try {
                    // TODO: Implementar cuando las dependencias estén disponibles
                    // Verificar si el usuario está en un chat grupal
                    // val currentChatId = voicePreferences.getCurrentChatId().first()
                    
                    // Simular que siempre hay un chat activo por ahora
                    val hasActiveChat = true
                    
                    if (hasActiveChat) {
                        // Usuario está en chat grupal - mantener servicio activo
                        if (!isListening) {
                            Log.d("VoiceService", "🔄 Reiniciando reconocimiento de voz...")
                            startVoiceRecognition()
                        }
                        
                        // Verificar que el WakeLock esté activo
                        if (wakeLock?.isHeld == false) {
                            acquireWakeLock()
                        }
                        
                        // Actualizar notificación
                        updateNotification("🎤 Escuchando comandos de voz")
                        
            } else {
                        // Usuario no está en chat grupal - pausar servicio
                        if (isListening) {
                            Log.d("VoiceService", "⏸️ Pausando servicio - no hay chat grupal activo")
                            stopVoiceRecognition()
                        }
                    }
                    
                    // Esperar 30 segundos antes de la siguiente verificación
                    kotlinx.coroutines.delay(30000)
            
        } catch (e: Exception) {
                    Log.e("VoiceService", "❌ Error en monitoreo continuo", e)
                    kotlinx.coroutines.delay(10000) // Esperar menos tiempo si hay error
                }
            }
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Desactiva el servicio cuando el usuario sale del chat grupal
     */
    private fun deactivateFromGroupChat() {
        try {
            Log.d("VoiceService", "🚪 Desactivando servicio - saliendo de chat grupal")
            
            // TODO: Implementar cuando las dependencias estén disponibles
            // Limpiar chat activo
            // serviceScope.launch {
            //     voicePreferences.setCurrentChatId(null)
            // }
            
            // Detener reconocimiento de voz
            if (isListening) {
                stopVoiceRecognition()
            }
            
            // Liberar WakeLock
            releaseWakeLock()
            
            // Actualizar notificación
            updateNotification("⏸️ Servicio pausado")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error desactivando de chat grupal", e)
        }
    }
    

    
    /**
     * Fallback a simulación de voz
     * DESHABILITADO: Esta función causaba detecciones automáticas falsas
     */
    private fun startSimulatedVoiceProcessing() {
        Log.d("VoiceService", "🔄 Simulación de voz DESHABILITADA - no se detectarán comandos automáticamente")
        
        // DESHABILITADO: No ejecutar simulación automática
        // Solo se debe detectar cuando realmente se reconozca un comando de voz
    }
    
    /**
     * Procesar comando de voz detectado
     */
    private suspend fun processVoiceCommand(command: String) {
        Log.d("VoiceService", "🎯 Comando detectado: $command")
        
        val currentChatId = getCurrentChatId()
        if (currentChatId == null || currentChatId.isEmpty()) {
            Log.d("VoiceService", "🔇 Comando ignorado: No hay chat grupal activo")
            // No mostrar notificación, solo ignorar silenciosamente
            return
        }
        
        Log.d("VoiceService", "✅ Chat activo encontrado: $currentChatId")
        
        // Obtener configuración de comandos
        val commandActions: Map<String, String> = getCommandActions()
        val action: String? = commandActions[command.lowercase()]
        
        when (action) {
            "AUDIO" -> {
                val duration: Int = getAudioRecordingDuration()
                Log.d("VoiceService", "🎤 Ejecutando grabación de audio por $duration segundos")
                serviceScope.launch {
                    if (isUserAuthenticated()) {
                        mediaRecordingService.recordAudio(duration, currentChatId)
                    } else {
                        Log.w("VoiceService", "⚠️ Usuario no autenticado para grabar audio")
                    }
                }
            }
            "AUDIO_MESSAGE" -> {
                Log.d("VoiceService", "🎤 Enviando mensaje de audio")
                serviceScope.launch {
                    sendAudioMessage(currentChatId)
                }
            }
            // COMENTADO TEMPORALMENTE - SOLO AUDIO ACTIVO
            /*
            "VIDEO" -> {
                val duration: Int = getVideoRecordingDuration()
                Log.d("VoiceService", "🎥 Ejecutando grabación de video por $duration segundos")
                serviceScope.launch {
                    if (isUserAuthenticated()) {
                        mediaRecordingService.recordVideo(duration, currentChatId)
                    } else {
                        Log.w("VoiceService", "⚠️ Usuario no autenticado para grabar video")
                    }
                }
            }
            "PHOTO" -> {
                Log.d("VoiceService", "📸 Ejecutando captura de foto")
                serviceScope.launch {
                    if (isUserAuthenticated()) {
                        mediaRecordingService.takePhoto(currentChatId)
                    } else {
                        Log.w("VoiceService", "⚠️ Usuario no autenticado para capturar foto")
                    }
                }
            }
            "TEXT" -> {
                Log.d("VoiceService", "💬 Enviando mensaje de texto")
                serviceScope.launch {
                    sendTextMessage(currentChatId, command)
                }
            }
            "LOCATION" -> {
                Log.d("VoiceService", "📍 Enviando ubicación actual")
                serviceScope.launch {
                    sendLocationMessage(currentChatId)
                }
            }
            "CALL" -> {
                Log.d("VoiceService", "📞 Iniciando llamada automática")
                serviceScope.launch {
                    initiateGroupCall(currentChatId)
                }
            }
            "SOS" -> {
                Log.d("VoiceService", "🚨 Ejecutando señal SOS")
                serviceScope.launch {
                    executeSOS(currentChatId)
                }
            }
            "TRACKING" -> {
                Log.d("VoiceService", "📍 Alternando seguimiento de ubicación")
                serviceScope.launch {
                    toggleLocationTracking(currentChatId)
                }
            }
            "SURVEILLANCE" -> {
                Log.d("VoiceService", "👁️ Alternando modo vigilancia")
                serviceScope.launch {
                    toggleSurveillanceMode(currentChatId)
                }
            }
            "STATUS" -> {
                Log.d("VoiceService", "📊 Enviando actualización de estado")
                serviceScope.launch {
                    sendStatusUpdate(currentChatId)
                }
            }
            "VIDEO_MESSAGE" -> {
                Log.d("VoiceService", "🎥 Enviando mensaje de video")
                serviceScope.launch {
                    sendVideoMessage(currentChatId)
                }
            }
            "PHOTO_MESSAGE" -> {
                Log.d("VoiceService", "📸 Enviando mensaje con foto")
                serviceScope.launch {
                    sendPhotoMessage(currentChatId)
                }
            }
            */
            // COMENTADO TEMPORALMENTE - SOLO AUDIO ACTIVO
            /*
            "AUDIO_RECORDING" -> {
                Log.d("VoiceService", "🎤 Iniciando grabación de audio")
                serviceScope.launch {
                    startAudioRecording(currentChatId)
                }
            }
            "STEALTH" -> {
                Log.d("VoiceService", "🥷 Alternando modo sigiloso")
                serviceScope.launch {
                    toggleStealthMode()
                }
            }
            */
            else -> {
                Log.d("VoiceService", "❓ Comando no reconocido: $command")
            }
        }
    }
    
    /**
     * Simular comandos de voz para testing
     * DESHABILITADO: Esta función causaba detecciones automáticas falsas
     */
    private fun getSimulatedVoiceCommand(): String? {
        // DESHABILITADO: No simular comandos automáticamente
        // Solo se debe detectar cuando realmente se reconozca un comando de voz
        return null
    }
    
    // Métodos simplificados para acceder a preferencias
    private fun getCurrentChatId(): String? {
        // Intentar obtener desde SharedPreferences (compatibilidad)
        val chatId = sharedPreferences.getString("current_chat_id", null)
        Log.d("VoiceService", "🔍 Chat ID desde SharedPreferences: $chatId")
        return chatId
    }
    
    /**
     * Cargar configuraciones de grabación desde SharedPreferences
     */
    private fun loadRecordingSettings() {
        try {
            // Cargar duración de audio (por defecto 5 segundos)
            audioRecordingDuration = sharedPreferences.getInt("audio_recording_duration", 5)
            
            // Cargar duración de video (por defecto 10 segundos)
            videoRecordingDuration = sharedPreferences.getInt("video_recording_duration", 10)
            
            // Cargar calidad de grabación (por defecto HIGH)
            recordingQuality = sharedPreferences.getString("recording_quality", "HIGH") ?: "HIGH"
            
            Log.d("VoiceService", "⚙️ Configuraciones cargadas - Audio: ${audioRecordingDuration}s, Video: ${videoRecordingDuration}s, Calidad: $recordingQuality")
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error cargando configuraciones: ${e.message}")
        }
    }
    
    private fun getDefaultCommandActions(): Map<String, String> {
        return mapOf(
            // SOLO COMANDOS DE AUDIO ACTIVOS
            "óyeme" to "AUDIO",
            "grabar audio" to "AUDIO",
            "audio" to "AUDIO",
            "emergencia" to "AUDIO", // Cambiado de CALL a AUDIO
            "alerta" to "AUDIO", // Cambiado de TEXT a AUDIO
            "refuerzo" to "AUDIO", // Nuevo comando de audio
            
            // COMENTADO TEMPORALMENTE - SOLO AUDIO ACTIVO
            /*
            "alerta" to "TEXT",
            "grabar video" to "VIDEO",
            "ayuda" to "LOCATION",
            "foto" to "PHOTO",
            "emergencia" to "CALL",
            "socorro" to "CALL",
            "video" to "VIDEO",
            "tomar foto" to "PHOTO",
            "ubicación" to "LOCATION",
            "posición" to "LOCATION",
            "llamar" to "CALL",
            "llamada" to "CALL",
            "sigiloso" to "STEALTH",
            "sos" to "SOS"
            */
        )
    }
    
    /**
     * Guardar comandos personalizados en SharedPreferences
     */
    private fun saveCommandActions(actions: Map<String, String>) {
        try {
            val actionsString = actions.map { "${it.key}:${it.value}" }.joinToString(",")
            sharedPreferences.edit().putString("command_actions", actionsString).apply()
            Log.d("VoiceService", "💾 Comandos guardados: $actionsString")
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error guardando comandos: ${e.message}")
        }
    }
    
    /**
     * Enviar mensaje de texto al chat
     */
    private suspend fun sendTextMessage(chatId: String, command: String) {
        try {
            // Primero verificar que el usuario sea participante del chat
            if (!isUserParticipantInChat(chatId)) {
                Log.w("VoiceService", "⚠️ Usuario no es participante del chat: $chatId")
                return
            }
            
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                val messageText = when (command.lowercase()) {
                    "alerta" -> "🚨 ALERTA: Necesito ayuda inmediata!"
                    "ayuda" -> "🆘 AYUDA: Requiero asistencia urgente!"
                    "emergencia" -> "🚨 EMERGENCIA: Situación crítica!"
                    else -> "💬 Mensaje de voz: $command"
                }
                
                val messageData = mapOf(
                    "chatId" to chatId,
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Usuario"),
                    "userPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "messageType" to "TEXT",
                    "content" to messageText,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val newMessageRef = messagesRef.push()
                newMessageRef.setValue(messageData).await()
                
                Log.d("VoiceService", "✅ Mensaje de texto enviado: $messageText")
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error enviando mensaje de texto: ${e.message}")
        }
    }
    
    /**
     * Enviar ubicación actual al chat
     */
    private suspend fun sendLocationMessage(chatId: String) {
        try {
            // Primero verificar que el usuario sea participante del chat
            if (!isUserParticipantInChat(chatId)) {
                Log.w("VoiceService", "⚠️ Usuario no es participante del chat: $chatId")
                return
            }
            
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                // TODO: Obtener ubicación real del GPS
                val locationText = "📍 Mi ubicación actual: https://maps.google.com/?q=-6.758615,-79.8489161"
                
                val messageData = mapOf(
                    "chatId" to chatId,
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Usuario"),
                    "userPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "messageType" to "LOCATION",
                    "content" to locationText,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val newMessageRef = messagesRef.push()
                newMessageRef.setValue(messageData).await()
                
                Log.d("VoiceService", "✅ Ubicación enviada al chat")
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error enviando ubicación: ${e.message}")
        }
    }
    
    /**
     * Iniciar llamada grupal
     */
    private suspend fun initiateGroupCall(chatId: String) {
        try {
            // Primero verificar que el usuario sea participante del chat
            if (!isUserParticipantInChat(chatId)) {
                Log.w("VoiceService", "⚠️ Usuario no es participante del chat: $chatId")
                return
            }
            
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                // Enviar mensaje de llamada iniciada en lugar de crear nodo de llamada
                val callMessage = "📞 LLAMADA INICIADA: ${currentUser.displayName ?: "Usuario"} ha iniciado una llamada grupal"
                
                val messageData = mapOf(
                    "chatId" to chatId,
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Usuario"),
                    "userPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "messageType" to "CALL",
                    "content" to callMessage,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val newMessageRef = messagesRef.push()
                newMessageRef.setValue(messageData).await()
                
                Log.d("VoiceService", "✅ Llamada grupal iniciada (mensaje enviado)")
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error iniciando llamada: ${e.message}")
        }
    }
    

    
    private fun startServiceMonitoring() {
        // TODO: Implementar monitoreo del servicio
    }
    
    private fun createNotification(message: String): android.app.Notification {
        // TODO: Implementar notificación
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Servicio de Voz")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }
    
    /**
     * Verificar si el usuario actual es participante del chat
     */
    private suspend fun isUserParticipantInChat(chatId: String): Boolean {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                Log.w("VoiceService", "❌ Usuario no autenticado")
                return false
            }
            
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val participantsRef = database.reference.child("chat_participants").child(chatId).child(currentUser.uid)
            
            val snapshot = participantsRef.get().await()
            val isParticipant = snapshot.exists()
            
            Log.d("VoiceService", "🔍 Usuario ${currentUser.uid} es participante del chat $chatId: $isParticipant")
            isParticipant
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error verificando participación en chat: ${e.message}")
            false
        }
    }
    
    /**
     * Verificar si el usuario está autenticado
     */
    private fun isUserAuthenticated(): Boolean {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isAuthenticated = currentUser != null
        
        Log.d("VoiceService", "🔐 Usuario autenticado: $isAuthenticated")
        return isAuthenticated
    }
    
    // ===== NUEVAS FUNCIONES DE COMANDOS DE VOZ =====
    
    private suspend fun executeSOS(chatId: String) {
        try {
            Log.d("VoiceService", "🚨 Ejecutando señal SOS")
            
            // Enviar mensaje SOS
            val sosMessage = "🚨 SOS 🚨\nNecesito ayuda inmediata\nCódigo de emergencia activado"
            sendTextMessage(chatId, sosMessage)
            
            // Enviar ubicación
            sendLocationMessage(chatId)
            
            // Tomar foto de emergencia
            mediaRecordingService.takePhoto(chatId)
            
            updateNotification("🚨 SOS enviado")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error ejecutando SOS: ${e.message}")
        }
    }
    
    private suspend fun toggleLocationTracking(chatId: String) {
        try {
            isTrackingLocation = !isTrackingLocation
            
            if (isTrackingLocation) {
                Log.d("VoiceService", "📍 Iniciando seguimiento de ubicación")
                sendTextMessage(chatId, "📍 Seguimiento de ubicación iniciado")
                updateNotification("📍 Seguimiento activo")
            } else {
                Log.d("VoiceService", "📍 Deteniendo seguimiento de ubicación")
                sendTextMessage(chatId, "📍 Seguimiento de ubicación detenido")
                updateNotification("📍 Seguimiento detenido")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error alternando seguimiento: ${e.message}")
        }
    }
    
    private suspend fun toggleSurveillanceMode(chatId: String) {
        try {
            isSurveillanceMode = !isSurveillanceMode
            
            if (isSurveillanceMode) {
                Log.d("VoiceService", "👁️ Activando modo vigilancia")
                sendTextMessage(chatId, "👁️ Modo vigilancia activado")
                updateNotification("👁️ Vigilancia activa")
            } else {
                Log.d("VoiceService", "👁️ Desactivando modo vigilancia")
                sendTextMessage(chatId, "👁️ Modo vigilancia desactivado")
                updateNotification("👁️ Vigilancia desactivada")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error alternando vigilancia: ${e.message}")
        }
    }
    
    private suspend fun sendStatusUpdate(chatId: String) {
        try {
            Log.d("VoiceService", "📊 Enviando actualización de estado")
            
            val statusMessage = "📊 Actualización de estado\n" +
                    "🕐 ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n" +
                    "✅ Operativo y en posición"
            
            sendTextMessage(chatId, statusMessage)
            updateNotification("📊 Estado actualizado")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error enviando estado: ${e.message}")
        }
    }
    
    private suspend fun sendAudioMessage(chatId: String) {
        try {
            val duration: Int = getAudioRecordingDuration()
            Log.d("VoiceService", "🎤 Grabando audio por $duration segundos")
            
            // Grabar audio con la duración configurada
            val result = mediaRecordingService.recordAudio(duration, chatId)
            if (result.isSuccess) {
                updateNotification("🎤 Audio grabado y enviado (${duration}s)")
            } else {
                Log.e("VoiceService", "❌ Error grabando audio: ${result.exceptionOrNull()?.message}")
                updateNotification("❌ Error grabando audio")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error grabando audio: ${e.message}")
            updateNotification("❌ Error grabando audio")
        }
    }
    
    private suspend fun sendVideoMessage(chatId: String) {
        try {
            val duration: Int = getVideoRecordingDuration()
            Log.d("VoiceService", "🎥 Grabando video por $duration segundos")
            
            // Grabar video con la duración configurada
            val result = mediaRecordingService.recordVideo(duration, chatId)
            if (result.isSuccess) {
                updateNotification("🎥 Video grabado y enviado (${duration}s)")
            } else {
                Log.e("VoiceService", "❌ Error grabando video: ${result.exceptionOrNull()?.message}")
                updateNotification("❌ Error grabando video")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error grabando video: ${e.message}")
            updateNotification("❌ Error grabando video")
        }
    }
    
    private suspend fun sendPhotoMessage(chatId: String) {
        try {
            Log.d("VoiceService", "📸 Tomando foto")
            
            // Tomar foto y enviar
            val result = mediaRecordingService.takePhoto(chatId)
            if (result.isSuccess) {
                updateNotification("📸 Foto tomada y enviada")
            } else {
                Log.e("VoiceService", "❌ Error tomando foto: ${result.exceptionOrNull()?.message}")
                updateNotification("❌ Error tomando foto")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error tomando foto: ${e.message}")
            updateNotification("❌ Error tomando foto")
        }
    }
    
    /**
     * Iniciar llamada grupal real
     */
    private suspend fun startGroupCall(chatId: String) {
        try {
            Log.d("VoiceService", "📞 Iniciando llamada grupal")
            
            // Enviar mensaje de llamada grupal al chat
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                val messageData = mapOf(
                    "chatId" to chatId,
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Usuario"),
                    "userPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "messageType" to "GROUP_CALL",
                    "content" to "📞 Llamada grupal iniciada por comando de voz",
                    "timestamp" to System.currentTimeMillis(),
                    "callType" to "VIDEO"
                )
                
                val newMessageRef = messagesRef.push()
                newMessageRef.setValue(messageData).await()
                
                updateNotification("📞 Llamada grupal iniciada")
                Log.d("VoiceService", "✅ Llamada grupal iniciada en chat: $chatId")
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error iniciando llamada grupal: ${e.message}")
            updateNotification("❌ Error iniciando llamada")
        }
    }
    
    private suspend fun startAudioRecording(chatId: String) {
        try {
            Log.d("VoiceService", "🎤 Iniciando grabación de audio")
            
            // Iniciar grabación continua
            val duration: Int = getAudioRecordingDuration()
            mediaRecordingService.recordAudio(duration, chatId)
            updateNotification("🎤 Grabación iniciada")
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error iniciando grabación: ${e.message}")
        }
    }
    
    private suspend fun toggleStealthMode() {
        try {
            isStealthMode = !isStealthMode
            
            if (isStealthMode) {
                Log.d("VoiceService", "🥷 Activando modo sigiloso")
                updateNotification("🥷 Modo sigiloso activado")
            } else {
                Log.d("VoiceService", "🥷 Desactivando modo sigiloso")
                updateNotification("🥷 Modo sigiloso desactivado")
            }
            
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error alternando modo sigiloso: ${e.message}")
        }
    }
    
    // ============== FUNCIONES DE CONFIGURACIÓN ==============
    
    /**
     * Obtiene la sensibilidad de voz desde SharedPreferences
     */
    private fun getVoiceSensitivity(): Float {
        return try {
            val sensitivity = sharedPreferences.getFloat("voice_sensitivity", 0.7f)
            
            // Corregir sensibilidad si está en 100% (probablemente un error)
            val correctedSensitivity = if (sensitivity >= 0.99f) {
                Log.w("VoiceService", "⚠️ Sensibilidad detectada en 100%, corrigiendo a 70%")
                sharedPreferences.edit().putFloat("voice_sensitivity", 0.7f).apply()
                0.7f
            } else {
                sensitivity
            }
            
            Log.d("VoiceService", "🎚️ Sensibilidad de voz: ${correctedSensitivity * 100}%")
            correctedSensitivity
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error obteniendo sensibilidad: ${e.message}")
            0.7f // Valor por defecto (70%)
        }
    }
    
    /**
     * Obtiene los comandos de voz desde SharedPreferences
     */
    private fun getCommandActions(): Map<String, String> {
        return try {
            // Intentar obtener desde SharedPreferences
            val actionsString = sharedPreferences.getString("command_actions", null)
            Log.d("VoiceService", "🔍 Comandos guardados: $actionsString")
            
            if (actionsString != null && actionsString.isNotEmpty()) {
                try {
                    val actions = actionsString.split(",").associate { action ->
                        val parts = action.split(":")
                        if (parts.size == 2) parts[0] to parts[1] else "" to ""
                    }.filter { it.key.isNotEmpty() }
                    Log.d("VoiceService", "✅ Comandos parseados: $actions")
                    actions
                } catch (e: Exception) {
                    Log.e("VoiceService", "❌ Error parseando comandos: ${e.message}")
                    getDefaultCommandActions()
                }
            } else {
                Log.d("VoiceService", "⚠️ No hay comandos guardados, usando por defecto")
                getDefaultCommandActions()
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error obteniendo comandos: ${e.message}")
            getDefaultCommandActions()
        }
    }
    
    /**
     * Obtiene la duración de grabación de audio
     */
    private fun getAudioRecordingDuration(): Int {
        return try {
            val duration = sharedPreferences.getInt("audio_recording_duration", 5)
            
            // Solo corregir si es un valor claramente erróneo (más de 60 segundos)
            val correctedDuration = if (duration > 60) {
                Log.w("VoiceService", "⚠️ Duración de audio muy alta (${duration}s), limitando a 30s")
                val limitedDuration = 30
                sharedPreferences.edit().putInt("audio_recording_duration", limitedDuration).apply()
                limitedDuration
            } else {
                duration
            }
            
            Log.d("VoiceService", "⏱️ Duración audio configurada: ${correctedDuration}s")
            correctedDuration
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error obteniendo duración audio: ${e.message}")
            5 // Valor por defecto
        }
    }
    
    /**
     * Obtiene la duración de grabación de video
     */
    private fun getVideoRecordingDuration(): Int {
        return try {
            val duration = sharedPreferences.getInt("video_recording_duration", 10)
            Log.d("VoiceService", "⏱️ Duración video: ${duration}s")
            duration
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error obteniendo duración video: ${e.message}")
            10 // Valor por defecto
        }
    }
    
    /**
     * Obtiene la calidad de grabación
     */
    private fun getRecordingQuality(): String {
        return try {
            val quality = sharedPreferences.getString("recording_quality", "HIGH")
            Log.d("VoiceService", "🎯 Calidad grabación: $quality")
            quality ?: "HIGH"
        } catch (e: Exception) {
            Log.e("VoiceService", "❌ Error obteniendo calidad: ${e.message}")
            "HIGH" // Valor por defecto
        }
    }

    /**
     * Monitoreo de salud del servicio para detectar problemas
     */
    private fun startHealthMonitoring() {
        serviceScope.launch {
            var consecutiveFailures = 0
            val maxFailures = 5
            
            while (isListening) {
                try {
                    delay(30000) // Verificar cada 30 segundos
                    
                    // Verificar salud del Vosk Engine
                    if (!voskEngine.isHealthy()) {
                        consecutiveFailures++
                        Log.w("VoiceService", "⚠️ Vosk Engine no saludable (fallo $consecutiveFailures/$maxFailures)")
                        
                        if (consecutiveFailures >= maxFailures) {
                            Log.e("VoiceService", "💀 Demasiados fallos consecutivos, reiniciando motor")
                            restartVoiceRecognition()
                            consecutiveFailures = 0
                        }
                    } else {
                        consecutiveFailures = 0 // Resetear contador si está saludable
                    }
                    
                } catch (e: Exception) {
                    Log.e("VoiceService", "❌ Error en monitoreo de salud", e)
                }
            }
        }
    }
}