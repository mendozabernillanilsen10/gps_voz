package com.example.demoappchat.data.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.demoappchat.MainActivity
import com.example.demoappchat.R
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.data.repository.FirebaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import android.util.Log
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest

/**
 * Servicio de fondo para detección de comandos de voz y creación automática de chats
 * Funciona incluso cuando la app está cerrada o en segundo plano
 */
@AndroidEntryPoint
class BackgroundVoiceService : Service() {

    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    
    // Configuración por defecto para chats automáticos
    private val defaultChatConfig = mapOf(
        "emergency" to ChatConfig(
            title = "🚨 Emergencia Automática",
            description = "Chat de emergencia creado por comando de voz",
            radius = 5000,
            pin = "1234",
            category = "emergency"
        ),
        "alert" to ChatConfig(
            title = "⚠️ Alerta Automática",
            description = "Chat de alerta activado por voz",
            radius = 3000,
            pin = "1234",
            category = "alert"
        ),
        "security" to ChatConfig(
            title = "🛡️ Vigilancia Automática", 
            description = "Chat de vigilancia activado por voz",
            radius = 4000,
            pin = "1234",
            category = "security"
        ),
        "recording" to ChatConfig(
            title = "🎤 Grabación Automática",
            description = "Chat de grabación activado por voz",
            radius = 2000,
            pin = "1234",
            category = "recording"
        ),
        "traffic" to ChatConfig(
            title = "🚦 Tráfico Automático",
            description = "Chat de tráfico activado por voz",
            radius = 2500,
            pin = "1234",
            category = "traffic"
        ),
        "community" to ChatConfig(
            title = "💬 Chat General Automático",
            description = "Chat general creado por comando de voz",
            radius = 3000,
            pin = "1234",
            category = "community"
        )
    )

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "background_voice_channel"
        const val ACTION_START_BACKGROUND = "START_BACKGROUND_VOICE"
        const val ACTION_STOP_BACKGROUND = "STOP_BACKGROUND_VOICE"
        const val ACTION_CREATE_CHAT = "CREATE_CHAT_FROM_VOICE"
    }

    data class ChatConfig(
        val title: String,
        val description: String,
        val radius: Int,
        val pin: String,
        val category: String
    )

    override fun onCreate() {
        super.onCreate()
        Log.d("BackgroundVoiceService", "🚀 Servicio de fondo de voz creado")
        
        createNotificationChannel()
        acquireWakeLock()
        initializeLocationManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundVoiceService", "📱 Comando recibido: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_BACKGROUND -> startBackgroundVoiceRecognition()
            ACTION_STOP_BACKGROUND -> stopBackgroundVoiceRecognition()
            ACTION_CREATE_CHAT -> {
                val chatType = intent.getStringExtra("chat_type") ?: "emergency"
                createChatFromVoiceCommand(chatType)
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBackgroundVoiceRecognition() {
        Log.d("BackgroundVoiceService", "🎤 Iniciando reconocimiento de voz en segundo plano")
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createChatFromVoiceCommand(chatType: String) {
        serviceScope.launch {
            try {
                Log.d("BackgroundVoiceService", "🏗️ Creando chat automático: $chatType")
                
                val config = defaultChatConfig[chatType] ?: defaultChatConfig["emergency"]!!
                val location = getCurrentLocation()
                
                val userId = firebaseRepository.getCurrentUserId()
                if (userId == null) {
                    Log.w("BackgroundVoiceService", "❌ Usuario no autenticado")
                    return@launch
                }
                
                val currentUser = firebaseRepository.currentUser.value
                val userName = currentUser?.name ?: "Usuario Automático"
                
                val chat = ProximityChat(
                    id = generateChatId(),
                    creatorId = userId,
                    creatorName = userName,
                    title = config.title,
                    description = config.description,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    radius = config.radius,
                    pin = config.pin,
                    createdAt = System.currentTimeMillis(),
                    isActive = true,
                    participantsCount = 1,
                    lastActivity = System.currentTimeMillis(),
                    category = config.category
                )
                
                // Crear chat en Firebase
                firebaseRepository.createProximityChatFromVoice(chat).onSuccess { createdChat ->
                    Log.d("BackgroundVoiceService", "✅ Chat creado exitosamente: ${createdChat.id}")
                    
                    // Registrar al usuario en el chat automáticamente
                    registerUserInChat(createdChat.id)
                    
                    // Comenzar grabación de audio automáticamente
                    startAutomaticAudioRecording(createdChat.id)
                    
                    // Notificar a otros usuarios
                    notifyOtherUsers(chatType, createdChat.id)
                    
                }.onFailure { error ->
                    Log.e("BackgroundVoiceService", "❌ Error creando chat", error)
                }
                
            } catch (e: Exception) {
                Log.e("BackgroundVoiceService", "❌ Error crítico creando chat", e)
            }
        }
    }

    /**
     * Registra al usuario actual en el chat grupal
     * Nota: FirebaseRepository.createProximityChatFromVoice ya registra automáticamente al creador
     */
    private suspend fun registerUserInChat(chatId: String) {
        try {
            Log.d("BackgroundVoiceService", "👤 Usuario ya registrado automáticamente en chat: $chatId")
            Log.d("BackgroundVoiceService", "✅ El creador del chat fue registrado durante la creación")
            
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "❌ Error en registro de usuario", e)
        }
    }
    
    /**
     * Comienza la grabación de audio automática
     */
    private suspend fun startAutomaticAudioRecording(chatId: String) {
        try {
            Log.d("BackgroundVoiceService", "🎤 Iniciando grabación automática en chat: $chatId")
            
            // Crear mensaje de sistema indicando que se está grabando
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            val recordingMessage = mapOf(
                "chatId" to chatId,
                "userId" to "system",
                "userName" to "Sistema",
                "userPhotoUrl" to "",
                "messageType" to "SYSTEM_RECORDING",
                "content" to "🎤 Grabación automática iniciada por comando de voz",
                "timestamp" to System.currentTimeMillis(),
                "autoRecording" to true,
                "recordingStatus" to "started"
            )
            
            val newMessageRef = messagesRef.push()
            newMessageRef.setValue(recordingMessage)
            
            Log.d("BackgroundVoiceService", "✅ Grabación automática iniciada")
            
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "❌ Error iniciando grabación automática", e)
        }
    }

    private fun generateChatId(): String {
        return "auto_${System.currentTimeMillis()}_${(0..999).random()}"
    }

    private fun getCurrentLocation(): Location? {
        if (currentLocation != null) return currentLocation
        
        // Obtener ubicación actual
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                currentLocation = location
                return location
            }
        }
        
        return null
    }

    private fun notifyOtherUsers(chatType: String, chatId: String? = null) {
        serviceScope.launch {
            try {
                Log.d("BackgroundVoiceService", "📢 Notificando a otros usuarios: $chatType")
                
                // Obtener configuración de radio de notificación según el tipo de chat
                val notificationRadius = getNotificationRadiusForChatType(chatType)
                
                // Enviar notificación FCM a usuarios cercanos
                val notificationData = mapOf(
                    "type" to "voice_command",
                    "chat_type" to chatType,
                    "chat_id" to (chatId ?: ""),
                    "timestamp" to System.currentTimeMillis().toString(),
                    "message" to "Nuevo comando de voz detectado: $chatType",
                    "notification_radius" to notificationRadius.toString(),
                    "priority" to "high"
                )
                
                // Notificar a usuarios dentro del radio configurado
                firebaseRepository.sendNotificationToNearbyUsers(notificationData, notificationRadius)
                
                // Reproducir sonido de notificación
                playNotificationSound()
                
                // Vibración
                vibrateDevice()
                
                Log.d("BackgroundVoiceService", "✅ Notificaciones enviadas a usuarios en radio de ${notificationRadius}m")
                
            } catch (e: Exception) {
                Log.e("BackgroundVoiceService", "❌ Error enviando notificaciones", e)
            }
        }
    }
    
    /**
     * Obtiene el radio de notificación en metros según el tipo de chat
     */
    private fun getNotificationRadiusForChatType(chatType: String): Int {
        return when (chatType) {
            "emergency" -> 5000  // 5km para emergencias
            "alert" -> 3000      // 3km para alertas
            "security" -> 4000   // 4km para seguridad/vigilancia
            "traffic" -> 2500    // 2.5km para tráfico
            "recording" -> 2000  // 2km para grabaciones
            "community" -> 3000  // 3km para chats comunitarios
            else -> 3000         // 3km por defecto
        }
    }

    private fun playNotificationSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            toneGen.release()
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "❌ Error reproduciendo sonido de notificación", e)
        }
    }

    private fun vibrateDevice() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "❌ Error en vibración", e)
        }
    }

    private fun stopBackgroundVoiceRecognition() {
        Log.d("BackgroundVoiceService", "🛑 Deteniendo reconocimiento de voz en segundo plano")
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitorea comandos de voz en segundo plano"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Voz Activa")
            .setContentText("Escuchando comandos de voz en segundo plano")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BackgroundVoiceService::VoiceWakeLock"
        )
        wakeLock?.acquire()
    }

    private fun initializeLocationManager() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000, // 10 segundos
                100f,  // 100 metros
                locationListener
            )
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BackgroundVoiceService", "💀 Servicio de fondo destruido")
        
        wakeLock?.release()
        locationManager?.removeUpdates(locationListener)
        serviceScope.cancel()
    }
}
