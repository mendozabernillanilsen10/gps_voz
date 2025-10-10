package com.example.demoappchat.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.demoappchat.MainActivity
import com.example.demoappchat.R
import com.example.demoappchat.presentation.chat.VideoCallActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * SERVICIO FCM PARA NOTIFICACIONES POLICIALES EN SEGUNDO PLANO
 * Maneja notificaciones de llamadas grupales y alertas de emergencia
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMessaging"
        
        // Canales de notificación
        const val CHANNEL_GROUP_CALLS = "group_calls"
        const val CHANNEL_EMERGENCY_ALERTS = "emergency_alerts"
        const val CHANNEL_VOICE_COMMANDS = "voice_commands"
        
        // Tipos de notificación
        const val TYPE_VIDEO_CALL = "video_call"
        const val TYPE_AUDIO_CALL = "audio_call"
        const val TYPE_EMERGENCY_ALERT = "emergency_alert"
        const val TYPE_VOICE_COMMAND = "voice_command"
        const val TYPE_PROXIMITY_CHAT = "proximity_chat_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Log.d(TAG, "🔥 Servicio FCM iniciado para operaciones policiales")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Nuevo token FCM: $token")
        
        // TODO: Enviar token al servidor para registro
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "📨 Mensaje FCM recibido desde: ${remoteMessage.from}")
        
        // Verificar datos del mensaje
        remoteMessage.data.let { data ->
            Log.d(TAG, "📋 Datos del mensaje: $data")
            
            val type = data["type"]
            when (type) {
                TYPE_VIDEO_CALL -> handleVideoCall(data)
                TYPE_AUDIO_CALL -> handleAudioCall(data)
                TYPE_EMERGENCY_ALERT -> handleEmergencyAlert(data)
                TYPE_VOICE_COMMAND -> handleVoiceCommand(data)
                TYPE_PROXIMITY_CHAT -> handleProximityChatAlert(data)
                else -> {
                    Log.w(TAG, "⚠️ Tipo de mensaje desconocido: $type")
                    // Manejar como notificación general
                    remoteMessage.notification?.let { notification ->
                        showGeneralNotification(
                            notification.title ?: "Notificación",
                            notification.body ?: "Nueva notificación recibida"
                        )
                    }
                }
            }
        }
    }

    private fun handleVideoCall(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val callerName = data["caller_name"] ?: "Usuario desconocido"
        val callId = data["call_id"] ?: return
        
        Log.d(TAG, "📹 Llamada de video entrante de: $callerName")
        
        // Intent para abrir la llamada de video
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra(VideoCallActivity.EXTRA_CHAT_ID, chatId)
            putExtra(VideoCallActivity.EXTRA_PARTICIPANT_NAME, callerName)
            putExtra(VideoCallActivity.EXTRA_IS_INCOMING_CALL, true)
            putExtra("call_id", callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent para rechazar llamada
        val rejectIntent = Intent().apply {
            action = "REJECT_CALL"
            putExtra("call_id", callId)
        }
        
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this,
            (callId + "_reject").hashCode(),
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_GROUP_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📹 Llamada grupal entrante")
            .setContentText("$callerName te está llamando")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Responder",
                pendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Rechazar",
                rejectPendingIntent
            )
            .setSound(null) // Usar sonido personalizado del canal
            .setVibrate(longArrayOf(0, 500, 1000, 500, 1000))
            .build()
        
        showNotification(callId.hashCode(), notification)
        
        // Auto-abrir la llamada para operaciones policiales críticas
        startActivity(intent)
    }

    private fun handleAudioCall(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val callerName = data["caller_name"] ?: "Usuario desconocido"
        val callId = data["call_id"] ?: return
        
        Log.d(TAG, "🎤 Llamada de audio entrante de: $callerName")
        
        // Similar al video call pero para audio
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra(VideoCallActivity.EXTRA_CHAT_ID, chatId)
            putExtra(VideoCallActivity.EXTRA_PARTICIPANT_NAME, callerName)
            putExtra(VideoCallActivity.EXTRA_IS_INCOMING_CALL, true)
            putExtra("call_id", callId)
            putExtra("audio_only", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_GROUP_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎤 Llamada de audio entrante")
            .setContentText("$callerName te está llamando")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .setVibrate(longArrayOf(0, 300, 600, 300, 600))
            .build()
        
        showNotification(callId.hashCode(), notification)
        startActivity(intent)
    }

    private fun handleEmergencyAlert(data: Map<String, String>) {
        val alertMessage = data["message"] ?: "Alerta de emergencia"
        val alertLevel = data["level"] ?: "medium"
        val location = data["location"]
        val agentName = data["agent_name"] ?: "Agente desconocido"
        
        Log.d(TAG, "🚨 ALERTA DE EMERGENCIA: $alertMessage")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("emergency_alert", true)
            putExtra("alert_message", alertMessage)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when (alertLevel) {
            "high" -> "🚨 EMERGENCIA CRÍTICA"
            "medium" -> "⚠️ ALERTA POLICIAL"
            else -> "📢 NOTIFICACIÓN"
        }
        
        val bodyText = buildString {
            append("$agentName: $alertMessage")
            if (location != null) {
                append("\n📍 Ubicación: $location")
            }
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .setVibrate(longArrayOf(0, 100, 200, 100, 200, 100, 200))
            .build()
        
        showNotification(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleVoiceCommand(data: Map<String, String>) {
        val command = data["command"] ?: return
        val agentName = data["agent_name"] ?: "Agente"
        
        Log.d(TAG, "🎤 Comando de voz recibido: $command")
        
        val notification = NotificationCompat.Builder(this, CHANNEL_VOICE_COMMANDS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎤 Comando activado")
            .setContentText("$agentName ejecutó: $command")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        showNotification(command.hashCode(), notification)
    }
    
    /**
     * ⭐ NUEVO: Manejar alertas de chats creados por voz
     * Con sonido fuerte, vibración y pantalla bloqueada
     */
    private fun handleProximityChatAlert(data: Map<String, String>) {
        val chatTitle = data["chatTitle"] ?: "Nuevo Chat"
        val creatorName = data["creatorName"] ?: "Usuario"
        val distance = data["distance"]?.toDouble()?.toInt() ?: 0
        val category = data["category"] ?: "community"
        val chatId = data["chatId"] ?: return
        
        Log.d(TAG, "🚨 ALERTA: Nuevo chat cercano - $chatTitle por $creatorName")
        
        // Intent para abrir el chat
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", chatId)
            putExtra("proximity_alert", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Determinar emoji e importancia según categoría
        val (emoji, priority, importance) = when (category) {
            "emergency" -> Triple("🚨", NotificationCompat.PRIORITY_MAX, NotificationManager.IMPORTANCE_HIGH)
            "alert" -> Triple("⚠️", NotificationCompat.PRIORITY_HIGH, NotificationManager.IMPORTANCE_HIGH)
            "security", "surveillance" -> Triple("🛡️", NotificationCompat.PRIORITY_HIGH, NotificationManager.IMPORTANCE_HIGH)
            "recording" -> Triple("🎤", NotificationCompat.PRIORITY_DEFAULT, NotificationManager.IMPORTANCE_DEFAULT)
            else -> Triple("💬", NotificationCompat.PRIORITY_DEFAULT, NotificationManager.IMPORTANCE_DEFAULT)
        }
        
        val title = "$emoji Chat Cercano"
        val bodyText = "$chatTitle\n👤 $creatorName • 📍 ${distance}m"
        
        // Configurar vibración fuerte para emergencias
        val vibrationPattern = when (category) {
            "emergency" -> longArrayOf(0, 300, 200, 300, 200, 300)  // Patrón de emergencia
            "alert" -> longArrayOf(0, 500, 300, 500)  // Patrón de alerta
            else -> longArrayOf(0, 400, 200, 400)  // Patrón normal
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // ⭐ SONIDO PREDETERMINADO DEL SISTEMA (fuerte)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            // ⭐ VIBRACIÓN FUERTE
            .setVibrate(vibrationPattern)
            // ⭐ MOSTRAR EN PANTALLA BLOQUEADA
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // ⭐ LUCES LED (si el dispositivo las tiene)
            .setLights(0xFFFF0000.toInt(), 1000, 1000)
            .build()
        
        showNotification(chatId.hashCode(), notification)
        
        Log.d(TAG, "✅ Notificación de chat mostrada con sonido y vibración")
    }

    private fun showGeneralNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_GROUP_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        showNotification(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal para llamadas grupales
            val callsChannel = NotificationChannel(
                CHANNEL_GROUP_CALLS,
                "Llamadas Grupales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de llamadas entrantes del grupo"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000, 500)
                setShowBadge(true)
            }
            
            // Canal para alertas de emergencia y chats por voz
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY_ALERTS,
                "Alertas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas críticas, emergencias y chats creados por voz"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 300)
                setShowBadge(true)
                // ⭐ SONIDO FUERTE
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // ⭐ MOSTRAR EN PANTALLA BLOQUEADA
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // ⭐ BYPASS "NO MOLESTAR" para emergencias
                setBypassDnd(true)
            }
            
            // Canal para comandos de voz
            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_COMMANDS,
                "Comandos de Voz",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de comandos de voz ejecutados"
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(callsChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(voiceChannel)
            
            Log.d(TAG, "✅ Canales de notificación creados")
        }
    }

    private fun showNotification(id: Int, notification: android.app.Notification) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "⚠️ Sin permisos para mostrar notificaciones")
            return
        }
        
        with(NotificationManagerCompat.from(this)) {
            notify(id, notification)
        }
        
        Log.d(TAG, "📱 Notificación mostrada con ID: $id")
    }

    private fun sendTokenToServer(token: String) {
        try {
            Log.d(TAG, "🔄 Registrando token FCM en Firebase: ${token.take(20)}...")
            
            // Guardar token localmente
            val sharedPrefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("fcm_token", token)
                .putLong("token_timestamp", System.currentTimeMillis())
                .apply()
            
            // 🆕 Registrar token en Firebase Database
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Actualizar token en el perfil del usuario
                database.getReference("users")
                    .child(currentUser.uid)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Token FCM registrado exitosamente en Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error registrando token FCM en Firebase", e)
                    }
                
                // También guardar en colección separada para notificaciones
                val tokenData = mapOf(
                    "userId" to currentUser.uid,
                    "token" to token,
                    "timestamp" to System.currentTimeMillis(),
                    "platform" to "android",
                    "appVersion" to "1.0.0"
                )
                
                database.getReference("fcm_tokens")
                    .child(currentUser.uid)
                    .setValue(tokenData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Token FCM guardado en colección separada")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error guardando token en colección separada", e)
                    }
            } else {
                Log.w(TAG, "⚠️ Usuario no autenticado, no se puede registrar token FCM")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sendTokenToServer", e)
        }
    }
}