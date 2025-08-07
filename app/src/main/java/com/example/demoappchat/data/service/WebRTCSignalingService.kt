package com.example.demoappchat.data.service

import android.util.Log
import com.example.demoappchat.data.model.*
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class WebRTCSignalingService(private val database: FirebaseDatabase) {
    
    private val callsRef = database.getReference("webrtc_calls")
    private val signalsRef = database.getReference("webrtc_signals")
    
    suspend fun startCall(
        chatId: String,
        initiatorId: String,
        initiatorName: String,
        callType: WebRTCCallType
    ): String {
        val callId = UUID.randomUUID().toString()
        
        val call = WebRTCCall(
            callId = callId,
            chatId = chatId,
            initiatorId = initiatorId,
            initiatorName = initiatorName,
            callType = callType,
            status = WebRTCCallStatus.RINGING,
            participants = mapOf(
                initiatorId to WebRTCCallParticipant(
                    userId = initiatorId,
                    userName = initiatorName,
                    isCaller = true,
                    isConnected = true
                )
            )
        )
        
        callsRef.child(callId).setValue(call).await()
        Log.d("WebRTCSignaling", "📞 Call started: $callId")
        return callId
    }
    
    suspend fun joinCall(callId: String, userId: String, userName: String): Boolean {
        return try {
            val participant = WebRTCCallParticipant(
                userId = userId,
                userName = userName,
                joinTime = System.currentTimeMillis(),
                isCaller = false,
                isConnected = true
            )
            
            callsRef.child(callId).child("participants").child(userId).setValue(participant).await()
            Log.d("WebRTCSignaling", "📞 User $userName joined call: $callId")
            true
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error joining call: ${e.message}")
            false
        }
    }
    
    suspend fun leaveCall(callId: String, userId: String) {
        try {
            callsRef.child(callId).child("participants").child(userId).child("leaveTime").setValue(System.currentTimeMillis()).await()
            Log.d("WebRTCSignaling", "📞 User $userId left call: $callId")
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error leaving call: ${e.message}")
        }
    }
    
    suspend fun endCall(callId: String) {
        try {
            callsRef.child(callId).child("status").setValue(WebRTCCallStatus.ENDED).await()
            callsRef.child(callId).child("endedAt").setValue(System.currentTimeMillis()).await()
            Log.d("WebRTCSignaling", "📞 Call ended: $callId")
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error ending call: ${e.message}")
        }
    }
    
    suspend fun sendSignal(signal: WebRTCSignal) {
        try {
            signalsRef.child(signal.signalId).setValue(signal).await()
            Log.d("WebRTCSignaling", "📡 Signal sent: ${signal.signalType}")
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error sending signal: ${e.message}")
        }
    }
    
    fun listenForSignals(callId: String, userId: String): Flow<WebRTCSignal> = callbackFlow {
        val listener = signalsRef.orderByChild("callId").equalTo(callId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val signal = child.getValue(WebRTCSignal::class.java)
                        if (signal != null && signal.toUserId == userId) {
                            trySend(signal)
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("WebRTCSignaling", "❌ Error listening for signals: ${error.message}")
                }
            })
        
        awaitClose {
            signalsRef.removeEventListener(listener)
        }
    }
    
    fun listenForCallChanges(callId: String): Flow<WebRTCCall> = callbackFlow {
        val listener = callsRef.child(callId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val call = snapshot.getValue(WebRTCCall::class.java)
                if (call != null) {
                    trySend(call)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("WebRTCSignaling", "❌ Error listening for call changes: ${error.message}")
            }
        })
        
        awaitClose {
            callsRef.child(callId).removeEventListener(listener)
        }
    }
    
    suspend fun getCallInfo(callId: String): WebRTCCall? {
        return try {
            val snapshot = callsRef.child(callId).get().await()
            snapshot.getValue(WebRTCCall::class.java)
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error getting call info: ${e.message}")
            null
        }
    }
    
    suspend fun cleanupOldSignals(callId: String) {
        try {
            signalsRef.orderByChild("callId").equalTo(callId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            child.ref.removeValue()
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("WebRTCSignaling", "❌ Error cleaning up signals: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error cleaning up signals: ${e.message}")
        }
    }
    
    // 🆕 NUEVAS FUNCIONES PARA QUE OTROS USUARIOS PUEDAN VER Y UNIRSE A LLAMADAS
    
    /**
     * Escuchar llamadas activas en un chat específico
     */
    fun listenForActiveCallsInChat(chatId: String): Flow<List<WebRTCCall>> = callbackFlow {
        val listener = callsRef
            .orderByChild("chatId")
            .equalTo(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activeCalls = mutableListOf<WebRTCCall>()
                    
                    for (child in snapshot.children) {
                        val call = child.getValue(WebRTCCall::class.java)
                        if (call != null && call.status != WebRTCCallStatus.ENDED) {
                            activeCalls.add(call)
                        }
                    }
                    
                    trySend(activeCalls)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("WebRTCSignaling", "❌ Error listening for active calls: ${error.message}")
                }
            })
        
        awaitClose {
            callsRef.removeEventListener(listener)
        }
    }
    
    /**
     * Obtener todas las llamadas activas en un chat
     */
    suspend fun getActiveCallsInChat(chatId: String): List<WebRTCCall> {
        return try {
            val snapshot = callsRef
                .orderByChild("chatId")
                .equalTo(chatId)
                .get()
                .await()
            
            val activeCalls = mutableListOf<WebRTCCall>()
            
            for (child in snapshot.children) {
                val call = child.getValue(WebRTCCall::class.java)
                if (call != null && call.status != WebRTCCallStatus.ENDED) {
                    activeCalls.add(call)
                }
            }
            
            Log.d("WebRTCSignaling", "📞 Found ${activeCalls.size} active calls in chat: $chatId")
            activeCalls
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error getting active calls: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Verificar si hay una llamada activa en un chat
     */
    suspend fun hasActiveCallInChat(chatId: String): Boolean {
        return try {
            val activeCalls = getActiveCallsInChat(chatId)
            activeCalls.isNotEmpty()
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error checking active calls: ${e.message}")
            false
        }
    }
    
    /**
     * Obtener la primera llamada activa en un chat
     */
    suspend fun getFirstActiveCallInChat(chatId: String): WebRTCCall? {
        return try {
            val activeCalls = getActiveCallsInChat(chatId)
            activeCalls.firstOrNull()
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error getting first active call: ${e.message}")
            null
        }
    }
    
    /**
     * Escuchar cambios en todas las llamadas de un chat
     */
    fun listenForAllCallChangesInChat(chatId: String): Flow<Map<String, WebRTCCall>> = callbackFlow {
        val listener = callsRef
            .orderByChild("chatId")
            .equalTo(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val callsMap = mutableMapOf<String, WebRTCCall>()
                    
                    for (child in snapshot.children) {
                        val call = child.getValue(WebRTCCall::class.java)
                        if (call != null) {
                            callsMap[call.callId] = call
                        }
                    }
                    
                    trySend(callsMap)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("WebRTCSignaling", "❌ Error listening for all call changes: ${error.message}")
                }
            })
        
        awaitClose {
            callsRef.removeEventListener(listener)
        }
    }
    
    /**
     * Limpiar llamadas terminadas automáticamente
     */
    suspend fun cleanupEndedCalls() {
        try {
            val snapshot = callsRef
                .orderByChild("status")
                .equalTo(WebRTCCallStatus.ENDED.name)
                .get()
                .await()
            
            for (child in snapshot.children) {
                val call = child.getValue(WebRTCCall::class.java)
                if (call != null && call.endedAt != null) {
                    // Eliminar llamadas terminadas hace más de 1 hora
                    val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                    if (call.endedAt < oneHourAgo) {
                        child.ref.removeValue().await()
                        Log.d("WebRTCSignaling", "🗑️ Cleaned up old call: ${call.callId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error cleaning up ended calls: ${e.message}")
        }
    }
    
    // 🆕 NUEVA FUNCIÓN: Notificar a todos los participantes de un chat
    suspend fun notifyChatParticipants(chatId: String, message: String, type: String = "call_notification") {
        try {
            Log.d("WebRTCSignaling", "📢 Notificando participantes del chat: $chatId")
            
            // Crear una notificación en la base de datos que otros usuarios puedan escuchar
            val notificationData = mapOf(
                "chatId" to chatId,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "senderId" to "system"
            )
            
            // Guardar en una colección de notificaciones
            val notificationsRef = database.getReference("chat_notifications")
            notificationsRef.child(chatId).push().setValue(notificationData).await()
            
            Log.d("WebRTCSignaling", "✅ Notificación enviada a participantes del chat")
            
        } catch (e: Exception) {
            Log.e("WebRTCSignaling", "❌ Error notificando participantes: ${e.message}")
        }
    }
    
    // 🆕 NUEVA FUNCIÓN: Escuchar notificaciones de chat
    fun listenForChatNotifications(chatId: String): Flow<Map<String, Any>> = callbackFlow {
        val listener = database.getReference("chat_notifications")
            .child(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val notification = child.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (notification != null) {
                            trySend(notification)
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("WebRTCSignaling", "❌ Error listening for chat notifications: ${error.message}")
                }
            })
        
        awaitClose {
            database.getReference("chat_notifications").child(chatId).removeEventListener(listener)
        }
    }
}
