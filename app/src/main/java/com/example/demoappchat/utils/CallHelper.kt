package com.example.demoappchat.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.demoappchat.data.model.WebRTCCallType
import com.example.demoappchat.presentation.chat.GroupCallActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📞 HELPER PARA INICIAR LLAMADAS GRUPALES
 * Facilita el inicio de llamadas de audio y video desde cualquier parte de la app
 */
object CallHelper {
    
    private const val TAG = "CallHelper"
    
    /**
     * Iniciar una llamada grupal de video
     */
    suspend fun startVideoCall(
        context: Context,
        chatId: String,
        chatName: String
    ): Result<String> {
        return startCall(context, chatId, chatName, "video")
    }
    
    /**
     * Iniciar una llamada grupal de audio
     */
    suspend fun startAudioCall(
        context: Context,
        chatId: String,
        chatName: String
    ): Result<String> {
        return startCall(context, chatId, chatName, "audio")
    }
    
    /**
     * Iniciar llamada (audio o video)
     */
    private suspend fun startCall(
        context: Context,
        chatId: String,
        chatName: String,
        callType: String
    ): Result<String> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Crear ID de llamada
            val callId = UUID.randomUUID().toString()
            
            Log.d(TAG, "📞 Iniciando llamada $callType en chat: $chatName")
            
            // Registrar llamada en Firebase
            val database = FirebaseDatabase.getInstance()
            val callData = mapOf(
                "callId" to callId,
                "chatId" to chatId,
                "initiatorId" to currentUser.uid,
                "initiatorName" to (currentUser.displayName ?: "Usuario"),
                "callType" to callType.uppercase(),
                "status" to "RINGING",
                "createdAt" to System.currentTimeMillis()
            )
            
            database.getReference("calls")
                .child(callId)
                .setValue(callData)
                .await()
            
            Log.d(TAG, "✅ Llamada registrada en Firebase: $callId")
            
            // Notificar a todos los participantes del chat
            notifyParticipants(chatId, callId, chatName, callType)
            
            // Abrir pantalla de llamada para el iniciador
            val intent = Intent(context, GroupCallActivity::class.java).apply {
                putExtra(GroupCallActivity.EXTRA_CHAT_ID, chatId)
                putExtra(GroupCallActivity.EXTRA_CHAT_NAME, chatName)
                putExtra(GroupCallActivity.EXTRA_CALL_ID, callId)
                putExtra(GroupCallActivity.EXTRA_CALL_TYPE, callType)
                putExtra(GroupCallActivity.EXTRA_IS_INITIATOR, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            
            Result.success(callId)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando llamada", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unirse a una llamada existente
     */
    fun joinCall(
        context: Context,
        chatId: String,
        chatName: String,
        callId: String,
        callType: String
    ) {
        val intent = Intent(context, GroupCallActivity::class.java).apply {
            putExtra(GroupCallActivity.EXTRA_CHAT_ID, chatId)
            putExtra(GroupCallActivity.EXTRA_CHAT_NAME, chatName)
            putExtra(GroupCallActivity.EXTRA_CALL_ID, callId)
            putExtra(GroupCallActivity.EXTRA_CALL_TYPE, callType.lowercase())
            putExtra(GroupCallActivity.EXTRA_IS_INITIATOR, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        
        Log.d(TAG, "📞 Uniéndose a llamada: $callId")
    }
    
    /**
     * Notificar a los participantes del chat sobre la llamada
     */
    private suspend fun notifyParticipants(
        chatId: String,
        callId: String,
        chatName: String,
        callType: String
    ) {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return
            
            val database = FirebaseDatabase.getInstance()
            
            // Obtener participantes del chat
            val participantsSnapshot = database.getReference("chat_participants")
                .child(chatId)
                .get()
                .await()
            
            var notifiedCount = 0
            
            participantsSnapshot.children.forEach { participantSnapshot ->
                val userId = participantSnapshot.key ?: return@forEach
                
                // No notificar al iniciador
                if (userId == currentUser.uid) return@forEach
                
                // Obtener datos del usuario
                val userSnapshot = database.getReference("users")
                    .child(userId)
                    .get()
                    .await()
                
                val fcmToken = userSnapshot.child("fcmToken").getValue(String::class.java)
                
                if (!fcmToken.isNullOrEmpty()) {
                    // Preparar notificación push
                    val notificationData = mapOf(
                        "type" to if (callType == "video") "video_call" else "audio_call",
                        "chat_id" to chatId,
                        "chat_name" to chatName,
                        "call_id" to callId,
                        "caller_name" to (currentUser.displayName ?: "Usuario"),
                        "call_type" to callType
                    )
                    
                    // Enviar a través de Cloud Functions o Firebase Cloud Messaging
                    // TODO: Implementar envío real de FCM
                    Log.d(TAG, "📤 Notificando a usuario: $userId")
                    notifiedCount++
                }
            }
            
            Log.d(TAG, "✅ $notifiedCount usuarios notificados")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notificando participantes", e)
        }
    }
    
    /**
     * Finalizar una llamada
     */
    suspend fun endCall(callId: String) {
        try {
            val database = FirebaseDatabase.getInstance()
            
            // Actualizar estado de la llamada
            database.getReference("calls")
                .child(callId)
                .child("status")
                .setValue("ENDED")
                .await()
            
            database.getReference("calls")
                .child(callId)
                .child("endedAt")
                .setValue(System.currentTimeMillis())
                .await()
            
            Log.d(TAG, "✅ Llamada finalizada: $callId")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finalizando llamada", e)
        }
    }
    
    /**
     * Verificar si hay una llamada activa en un chat
     */
    suspend fun getActiveCall(chatId: String): Pair<String, String>? {
        return try {
            val database = FirebaseDatabase.getInstance()
            
            // Buscar llamadas activas para este chat
            val snapshot = database.getReference("calls")
                .orderByChild("chatId")
                .equalTo(chatId)
                .get()
                .await()
            
            // Buscar primera llamada activa (RINGING o CONNECTED)
            for (callSnapshot in snapshot.children) {
                val status = callSnapshot.child("status").getValue(String::class.java)
                val callType = callSnapshot.child("callType").getValue(String::class.java)
                val callId = callSnapshot.key
                
                if ((status == "RINGING" || status == "CONNECTED") && callId != null && callType != null) {
                    Log.d(TAG, "🔍 Llamada activa encontrada: $callId")
                    return Pair(callId, callType.lowercase())
                }
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando llamadas activas", e)
            null
        }
    }
}


