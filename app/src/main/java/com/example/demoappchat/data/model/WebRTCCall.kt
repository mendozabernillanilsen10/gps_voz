package com.example.demoappchat.data.model

import com.google.gson.annotations.SerializedName

// WebRTC Call Models - Separate from GroupCall to avoid conflicts
data class WebRTCCall(
    val callId: String = "",
    val chatId: String = "",
    val initiatorId: String = "",
    val initiatorName: String = "",
    val callType: WebRTCCallType = WebRTCCallType.AUDIO,
    val status: WebRTCCallStatus = WebRTCCallStatus.RINGING,
    val participants: Map<String, WebRTCCallParticipant> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val iceServers: List<IceServer> = emptyList()
)

data class WebRTCCallParticipant(
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val joinTime: Long = System.currentTimeMillis(),
    val leaveTime: Long? = null,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val connectionStatus: WebRTCConnectionStatus = WebRTCConnectionStatus.CONNECTING,
    val isCaller: Boolean = false,
    val isConnected: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val leftAt: Long? = null,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
)

enum class WebRTCCallType {
    AUDIO, VIDEO
}

enum class WebRTCCallStatus {
    RINGING, CONNECTING, CONNECTED, ENDED, REJECTED, MISSED
}

enum class WebRTCConnectionStatus {
    CONNECTING, CONNECTED, DISCONNECTED, FAILED
}

enum class ConnectionQuality {
    UNKNOWN, POOR, FAIR, GOOD, EXCELLENT
}

// WebRTC Signaling Models
data class WebRTCSignal(
    val signalId: String = "",
    val callId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val signalType: SignalType = SignalType.OFFER,
    val data: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalType {
    OFFER, ANSWER, ICE_CANDIDATE, HANGUP, MUTE, UNMUTE, VIDEO_ON, VIDEO_OFF
}

data class IceCandidate(
    val sdp: String = "",
    val sdpMLineIndex: Int = 0,
    val sdpMid: String = ""
)

data class SessionDescription(
    val type: String = "",
    val description: String = ""
)

data class SignalingResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
)

data class RTCConfiguration(
    val iceServers: List<IceServer> = emptyList(),
    val iceCandidatePoolSize: Int = 10
)

data class IceServer(
    val urls: List<String> = emptyList(),
    val username: String = "",
    val credential: String = ""
)
