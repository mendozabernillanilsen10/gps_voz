package com.example.demoappchat.data.model

data class GroupCall(
    val callId: String = "",
    val chatId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callType: CallType = CallType.VIDEO,
    val status: CallStatus = CallStatus.INITIATING,
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val participants: Map<String, CallParticipant> = emptyMap(),
    val maxParticipants: Int = 10
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", CallType.VIDEO, CallStatus.INITIATING, 0L, null, emptyMap(), 10)
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "callId" to callId,
            "chatId" to chatId,
            "callerId" to callerId,
            "callerName" to callerName,
            "callType" to callType.name,
            "status" to status.name,
            "startTime" to startTime,
            "endTime" to (endTime ?: 0L),
            "maxParticipants" to maxParticipants
        )
    }
}

data class CallParticipant(
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val joinTime: Long = 0L,
    val leaveTime: Long? = null,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", 0L, null, false, true, ConnectionStatus.CONNECTING)
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "joinTime" to joinTime,
            "leaveTime" to (leaveTime ?: 0L),
            "isMuted" to isMuted,
            "isVideoEnabled" to isVideoEnabled,
            "connectionStatus" to connectionStatus.name
        )
    }
}

enum class CallType {
    AUDIO, VIDEO
}

enum class CallStatus {
    INITIATING, ACTIVE, ENDED, MISSED
}

enum class ConnectionStatus {
    CONNECTING, CONNECTED, DISCONNECTED, FAILED
} 