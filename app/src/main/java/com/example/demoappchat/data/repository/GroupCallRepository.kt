package com.example.demoappchat.data.repository

import com.example.demoappchat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupCallRepository @Inject constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val callsRef = database.reference.child("group_calls")
    private val notificationsRef = database.reference.child("call_notifications")
    
    /**
     * Iniciar una nueva llamada grupal
     */
    suspend fun startGroupCall(
        chatId: String,
        callType: CallType,
        maxParticipants: Int = 10
    ): Result<GroupCall> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val callId = callsRef.child(chatId).push().key
                ?: return Result.failure(Exception("Error generando ID de llamada"))
            
            val call = GroupCall(
                callId = callId,
                chatId = chatId,
                callerId = currentUser.uid,
                callerName = currentUser.displayName ?: "Usuario",
                callType = callType,
                status = CallStatus.INITIATING,
                startTime = System.currentTimeMillis(),
                maxParticipants = maxParticipants
            )
            
            // Crear la llamada en Firebase
            callsRef.child(chatId).child(callId).setValue(call.toMap()).await()
            
            // Agregar al llamador como primer participante
            val participant = CallParticipant(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Usuario",
                userPhotoUrl = currentUser.photoUrl?.toString() ?: "",
                joinTime = System.currentTimeMillis(),
                connectionStatus = ConnectionStatus.CONNECTED
            )
            
            callsRef.child(chatId).child(callId)
                .child("participants").child(currentUser.uid)
                .setValue(participant.toMap()).await()
            
            Result.success(call)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unirse a una llamada existente
     */
    suspend fun joinCall(chatId: String, callId: String): Result<CallParticipant> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val participant = CallParticipant(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Usuario",
                userPhotoUrl = currentUser.photoUrl?.toString() ?: "",
                joinTime = System.currentTimeMillis(),
                connectionStatus = ConnectionStatus.CONNECTING
            )
            
            callsRef.child(chatId).child(callId)
                .child("participants").child(currentUser.uid)
                .setValue(participant.toMap()).await()
            
            // Actualizar estado de la llamada a activa si es el primer participante que se une
            callsRef.child(chatId).child(callId).child("status")
                .setValue(CallStatus.ACTIVE.name).await()
            
            Result.success(participant)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Salir de una llamada
     */
    suspend fun leaveCall(chatId: String, callId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Marcar tiempo de salida
            callsRef.child(chatId).child(callId)
                .child("participants").child(currentUser.uid)
                .child("leaveTime").setValue(System.currentTimeMillis()).await()
            
            // Actualizar estado de conexión
            callsRef.child(chatId).child(callId)
                .child("participants").child(currentUser.uid)
                .child("connectionStatus").setValue(ConnectionStatus.DISCONNECTED.name).await()
            
            // Verificar si quedan participantes activos
            val participantsSnapshot = callsRef.child(chatId).child(callId)
                .child("participants").get().await()
            
            val activeParticipants = participantsSnapshot.children.count { participantSnapshot ->
                val status = participantSnapshot.child("connectionStatus").getValue(String::class.java)
                status != ConnectionStatus.DISCONNECTED.name
            }
            
            // Si no quedan participantes activos, terminar la llamada
            if (activeParticipants <= 1) {
                endCall(chatId, callId)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Terminar una llamada
     */
    suspend fun endCall(chatId: String, callId: String): Result<Unit> {
        return try {
            callsRef.child(chatId).child(callId).child("status")
                .setValue(CallStatus.ENDED.name).await()
            
            callsRef.child(chatId).child(callId).child("endTime")
                .setValue(System.currentTimeMillis()).await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener llamada activa de un chat
     */
    suspend fun getActiveCall(chatId: String): GroupCall? {
        return try {
            val snapshot = callsRef.child(chatId).orderByChild("status")
                .equalTo(CallStatus.ACTIVE.name).get().await()
            
            if (snapshot.exists()) {
                val callSnapshot = snapshot.children.first()
                parseGroupCall(callSnapshot)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Observar cambios en una llamada específica
     */
    fun observeCall(chatId: String, callId: String): Flow<GroupCall?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val call = if (snapshot.exists()) {
                    parseGroupCall(snapshot)
                } else {
                    null
                }
                trySend(call)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        callsRef.child(chatId).child(callId).addValueEventListener(listener)
        
        awaitClose {
            callsRef.child(chatId).child(callId).removeEventListener(listener)
        }
    }
    
    /**
     * Observar llamadas activas en un chat
     */
    fun observeActiveCalls(chatId: String): Flow<List<GroupCall>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = snapshot.children.mapNotNull { callSnapshot ->
                    val status = callSnapshot.child("status").getValue(String::class.java)
                    if (status == CallStatus.ACTIVE.name || status == CallStatus.INITIATING.name) {
                        parseGroupCall(callSnapshot)
                    } else {
                        null
                    }
                }
                trySend(calls)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        callsRef.child(chatId).addValueEventListener(listener)
        
        awaitClose {
            callsRef.child(chatId).removeEventListener(listener)
        }
    }
    
    /**
     * Actualizar estado de un participante
     */
    suspend fun updateParticipantStatus(
        chatId: String,
        callId: String,
        userId: String,
        isMuted: Boolean? = null,
        isVideoEnabled: Boolean? = null,
        connectionStatus: ConnectionStatus? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            
            isMuted?.let { updates["isMuted"] = it }
            isVideoEnabled?.let { updates["isVideoEnabled"] = it }
            connectionStatus?.let { updates["connectionStatus"] = it.name }
            
            if (updates.isNotEmpty()) {
                callsRef.child(chatId).child(callId)
                    .child("participants").child(userId)
                    .updateChildren(updates).await()
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Enviar notificación de llamada a participantes
     */
    suspend fun sendCallNotification(
        chatId: String,
        callId: String,
        recipientIds: List<String>
    ): Result<Unit> {
        return try {
            val notificationData = mapOf(
                "callId" to callId,
                "chatId" to chatId,
                "timestamp" to System.currentTimeMillis()
            )
            
            recipientIds.forEach { recipientId ->
                notificationsRef.child(chatId).child(callId)
                    .child("recipients").child(recipientId)
                    .setValue(notificationData).await()
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseGroupCall(snapshot: DataSnapshot): GroupCall? {
        return try {
            val callId = snapshot.key ?: return null
            val chatId = snapshot.child("chatId").getValue(String::class.java) ?: return null
            val callerId = snapshot.child("callerId").getValue(String::class.java) ?: return null
            val callerName = snapshot.child("callerName").getValue(String::class.java) ?: ""
            val callTypeStr = snapshot.child("callType").getValue(String::class.java) ?: "VIDEO"
            val statusStr = snapshot.child("status").getValue(String::class.java) ?: "INITIATING"
            val startTime = snapshot.child("startTime").getValue(Long::class.java) ?: 0L
            val endTime = snapshot.child("endTime").getValue(Long::class.java)
            val maxParticipants = snapshot.child("maxParticipants").getValue(Int::class.java) ?: 10
            
            val callType = try {
                CallType.valueOf(callTypeStr)
            } catch (e: Exception) {
                CallType.VIDEO
            }
            
            val status = try {
                CallStatus.valueOf(statusStr)
            } catch (e: Exception) {
                CallStatus.INITIATING
            }
            
            // Parsear participantes
            val participantsSnapshot = snapshot.child("participants")
            val participants = mutableMapOf<String, CallParticipant>()
            
            participantsSnapshot.children.forEach { participantSnapshot ->
                val participant = parseCallParticipant(participantSnapshot)
                if (participant != null) {
                    participants[participant.userId] = participant
                }
            }
            
            GroupCall(
                callId = callId,
                chatId = chatId,
                callerId = callerId,
                callerName = callerName,
                callType = callType,
                status = status,
                startTime = startTime,
                endTime = endTime,
                participants = participants,
                maxParticipants = maxParticipants
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseCallParticipant(snapshot: DataSnapshot): CallParticipant? {
        return try {
            val userId = snapshot.key ?: return null
            val userName = snapshot.child("userName").getValue(String::class.java) ?: ""
            val userPhotoUrl = snapshot.child("userPhotoUrl").getValue(String::class.java) ?: ""
            val joinTime = snapshot.child("joinTime").getValue(Long::class.java) ?: 0L
            val leaveTime = snapshot.child("leaveTime").getValue(Long::class.java)
            val isMuted = snapshot.child("isMuted").getValue(Boolean::class.java) ?: false
            val isVideoEnabled = snapshot.child("isVideoEnabled").getValue(Boolean::class.java) ?: true
            val statusStr = snapshot.child("connectionStatus").getValue(String::class.java) ?: "CONNECTING"
            
            val connectionStatus = try {
                ConnectionStatus.valueOf(statusStr)
            } catch (e: Exception) {
                ConnectionStatus.CONNECTING
            }
            
            CallParticipant(
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                joinTime = joinTime,
                leaveTime = leaveTime,
                isMuted = isMuted,
                isVideoEnabled = isVideoEnabled,
                connectionStatus = connectionStatus
            )
            
        } catch (e: Exception) {
            null
        }
    }
} 