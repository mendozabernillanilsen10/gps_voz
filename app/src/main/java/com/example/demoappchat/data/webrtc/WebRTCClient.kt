package com.example.demoappchat.data.webrtc

import android.content.Context
import android.util.Log
import com.example.demoappchat.data.model.*
import com.example.demoappchat.data.service.WebRTCSignalingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class WebRTCClient(
    private val context: Context,
    private val signalingService: WebRTCSignalingService
) {
    
    private val _callState = MutableStateFlow<WebRTCCallStatus>(WebRTCCallStatus.ENDED)
    val callState: StateFlow<WebRTCCallStatus> = _callState
    
    private val _participants = MutableStateFlow<Map<String, WebRTCCallParticipant>>(emptyMap())
    val participants: StateFlow<Map<String, WebRTCCallParticipant>> = _participants
    
    private var currentCallId: String? = null
    private var currentUserId: String? = null
    
    // Callbacks
    var onCallStateChanged: ((WebRTCCallStatus) -> Unit)? = null
    var onParticipantJoined: ((WebRTCCallParticipant) -> Unit)? = null
    var onParticipantLeft: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    fun initializeWebRTC() {
        try {
            Log.d("WebRTCClient", "✅ WebRTC client initialized")
        } catch (e: Exception) {
            Log.e("WebRTCClient", "❌ Error initializing WebRTC: ${e.message}")
            onError?.invoke("Failed to initialize WebRTC: ${e.message}")
        }
    }
    
    suspend fun startCall(
        chatId: String,
        userId: String,
        userName: String,
        callType: WebRTCCallType
    ): String? {
        return try {
            currentUserId = userId
            val callId = signalingService.startCall(chatId, userId, userName, callType)
            currentCallId = callId
            
            _callState.value = WebRTCCallStatus.RINGING
            onCallStateChanged?.invoke(WebRTCCallStatus.RINGING)
            
            Log.d("WebRTCClient", "📞 Call started: $callId")
            callId
        } catch (e: Exception) {
            Log.e("WebRTCClient", "❌ Error starting call: ${e.message}")
            onError?.invoke("Failed to start call: ${e.message}")
            null
        }
    }
    
    suspend fun joinCall(callId: String, userId: String, userName: String): Boolean {
        return try {
            currentCallId = callId
            currentUserId = userId
            
            val success = signalingService.joinCall(callId, userId, userName)
            if (success) {
                _callState.value = WebRTCCallStatus.CONNECTING
                onCallStateChanged?.invoke(WebRTCCallStatus.CONNECTING)
                
                Log.d("WebRTCClient", "📞 Joined call: $callId")
            }
            success
        } catch (e: Exception) {
            Log.e("WebRTCClient", "❌ Error joining call: ${e.message}")
            onError?.invoke("Failed to join call: ${e.message}")
            false
        }
    }
    
    fun handleParticipantJoined(participant: WebRTCCallParticipant) {
        _participants.value = _participants.value + (participant.userId to participant)
        onParticipantJoined?.invoke(participant)
    }
    
    fun handleParticipantLeft(userId: String) {
        _participants.value = _participants.value - userId
        onParticipantLeft?.invoke(userId)
    }
    
    fun endCall() {
        try {
            currentCallId?.let { callId ->
                // Note: This would need to be called in a coroutine context
                // For now, we'll just update the state
            }
            
            _callState.value = WebRTCCallStatus.ENDED
            onCallStateChanged?.invoke(WebRTCCallStatus.ENDED)
            
            Log.d("WebRTCClient", "📞 Call ended")
        } catch (e: Exception) {
            Log.e("WebRTCClient", "❌ Error ending call: ${e.message}")
        }
    }
    
    fun toggleMute() {
        Log.d("WebRTCClient", "🎤 Toggle mute")
        // Implementation would go here
    }
    
    fun toggleVideo() {
        Log.d("WebRTCClient", "📹 Toggle video")
        // Implementation would go here
    }
    
    fun setCallbacks(
        onCallStateChanged: ((WebRTCCallStatus) -> Unit)? = null,
        onParticipantJoined: ((WebRTCCallParticipant) -> Unit)? = null,
        onParticipantLeft: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.onCallStateChanged = onCallStateChanged
        this.onParticipantJoined = onParticipantJoined
        this.onParticipantLeft = onParticipantLeft
        this.onError = onError
    }
    
    fun dispose() {
        try {
            Log.d("WebRTCClient", "✅ WebRTC client disposed")
        } catch (e: Exception) {
            Log.e("WebRTCClient", "❌ Error disposing WebRTC client: ${e.message}")
        }
    }
}
