package com.example.demoappchat.presentation.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.data.model.CallType
import com.example.demoappchat.data.model.WebRTCCallType
import com.example.demoappchat.data.model.WebRTCCall
import com.example.demoappchat.data.model.WebRTCCallStatus
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.service.VoiceRecognitionService
import com.example.demoappchat.data.service.WebRTCSignalingService
import com.example.demoappchat.data.webrtc.WebRTCClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val userPreferences: UserPreferences,
    private val webRTCClient: WebRTCClient,
    private val signalingService: WebRTCSignalingService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val currentUser = repository.currentUser
    
    // Getter para el ID de la llamada grupal
    val groupCallId: String?
        get() = _uiState.value.groupCallId
    
    // 🆕 Estados para llamadas activas
    private val _activeCalls = MutableStateFlow<List<WebRTCCall>>(emptyList())
    val activeCalls: StateFlow<List<WebRTCCall>> = _activeCalls.asStateFlow()
    
    private val _hasActiveCall = MutableStateFlow(false)
    val hasActiveCall: StateFlow<Boolean> = _hasActiveCall.asStateFlow()

    fun loadChat(chatId: String) {
        viewModelScope.launch {
            // Cargar información del chat
            // Implementar método en repository para obtener chat específico
            
            // 🆕 Iniciar monitoreo de llamadas activas en este chat
            startMonitoringActiveCalls(chatId)
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            repository.getChatMessages(chatId).collect { messagesList ->
                _messages.value = messagesList
            }
        }
    }

    fun sendMessage(chatId: String, content: String, type: MessageType) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val message = ChatMessage(
                    chatId = chatId,
                    userId = user.id,
                    userName = user.name,
                    userPhotoUrl = user.photoUrl,
                    messageType = type,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )

                repository.sendMessage(message)
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message
                        )
                    }
            }
        }
    }

    // ✅ REMOVIDO 'suspend' - ahora es una función regular
    fun uploadAndSendMedia(chatId: String, uri: Uri, type: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val file = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = java.io.File.createTempFile(
                        type + "_",
                        when (type) {
                            "audio" -> ".mp3"
                            "video" -> ".mp4"
                            "photo", "image" -> ".jpg"
                            else -> ".jpg"
                        },
                        context.cacheDir
                    )
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }

                // Subir archivo y obtener URL
                val downloadUrl = repository.uploadMediaFile(file, type, chatId)

                // Enviar mensaje con media
                repository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = downloadUrl,
                    messageType = when (type) {
                        "audio" -> "AUDIO"
                        "video" -> "VIDEO"
                        "photo", "image" -> "PHOTO"
                        else -> "PHOTO"
                    },
                    content = when (type) {
                        "audio" -> "Audio enviado"
                        "video" -> "Video enviado"
                        "photo", "image" -> "Foto enviada"
                        else -> "Foto enviada"
                    }
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error subiendo archivo", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error subiendo ${type}: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // Voice service integration methods
    fun setCurrentChatId(chatId: String) {
        viewModelScope.launch {
            // Guardar en DataStore
            userPreferences.setCurrentChatId(chatId)
            
            // También guardar en SharedPreferences para el servicio de voz
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("current_chat_id", chatId).apply()
            
            Log.d("ChatViewModel", "💾 Chat ID guardado: $chatId (DataStore + SharedPreferences)")
        }
    }
    
    fun clearCurrentChatId() {
        viewModelScope.launch {
            // Limpiar en DataStore
            userPreferences.setCurrentChatId(null)
            
            // También limpiar en SharedPreferences
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("current_chat_id").apply()
            
            Log.d("ChatViewModel", "🗑️ Chat ID limpiado (DataStore + SharedPreferences)")
        }
    }
    
    // TODO: Implementar cuando las dependencias estén disponibles
    // fun startGroupVideoCall(chatId: String) {
    //     viewModelScope.launch {
    //         // Implementar lógica para iniciar videollamada grupal
    //         Log.d("ChatViewModel", "📹 Iniciando videollamada grupal: $chatId")
    //     }
    // }
    //
    // fun startGroupAudioCall(chatId: String) {
    //     viewModelScope.launch {
    //         // Implementar lógica para iniciar llamada de audio grupal
    //         Log.d("ChatViewModel", "🎤 Iniciando llamada de audio grupal: $chatId")
    //     }
    // }
    
    // ============== FCM GROUP CALLS INTEGRATION ==============
    
    /**
     * Iniciar videollamada grupal con notificaciones FCM
     */
    fun startGroupVideoCall(chatId: String) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    // Notificar a todos los participantes del chat
                    signalingService.notifyChatParticipants(
                        chatId = chatId,
                        message = "📹 ${user.name} inició una videollamada grupal",
                        type = "video_call_started"
                    )
                    
                    // Iniciar la llamada WebRTC
                    val callId = webRTCClient.startCall(
                        chatId = chatId,
                        userId = user.id,
                        userName = user.name,
                        callType = WebRTCCallType.VIDEO
                    )
                    
                    if (callId != null) {
                        sendGroupCallMessage(chatId, "📹 Videollamada iniciada", "video")
                        
                        _uiState.value = _uiState.value.copy(
                            isGroupCallActive = true,
                            groupCallType = "video",
                            groupCallId = callId
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Error al iniciar videollamada"
                        )
                    }
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        error = "Usuario no autenticado"
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error en videollamada grupal", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Iniciar llamada de audio grupal con notificaciones FCM
     */
    fun startGroupAudioCall(chatId: String) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    // Notificar a todos los participantes del chat
                    signalingService.notifyChatParticipants(
                        chatId = chatId,
                        message = "🎤 ${user.name} inició una llamada de audio grupal",
                        type = "audio_call_started"
                    )
                    
                    // Iniciar la llamada WebRTC
                    val callId = webRTCClient.startCall(
                        chatId = chatId,
                        userId = user.id,
                        userName = user.name,
                        callType = WebRTCCallType.AUDIO
                    )
                    
                    if (callId != null) {
                        sendGroupCallMessage(chatId, "🎤 Llamada de audio iniciada", "audio")
                        
                        _uiState.value = _uiState.value.copy(
                            isGroupCallActive = true,
                            groupCallType = "audio",
                            groupCallId = callId
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Error al iniciar llamada de audio"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error en llamada de audio grupal", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Enviar mensaje del sistema sobre llamadas grupales
     */
    private suspend fun sendGroupCallMessage(chatId: String, content: String, callType: String) {
        try {
            currentUser.value?.let { user ->
                val callMessage = ChatMessage(
                    chatId = chatId,
                    userId = "SYSTEM_CALL", // ID especial para mensajes de llamadas
                    userName = "📞 Sistema de Llamadas",
                    userPhotoUrl = "",
                    messageType = MessageType.SYSTEM,
                    content = "$content\n\n🚔 Todos los agentes del grupo han sido notificados automáticamente",
                    timestamp = System.currentTimeMillis()
                )
                
                repository.sendMessage(callMessage)
                Log.d("ChatViewModel", "✅ Mensaje de llamada $callType enviado al chat")
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "❌ Error enviando mensaje de llamada", e)
        }
    }
    
    /**
     * Enviar alerta de emergencia FCM
     */
    fun sendEmergencyAlert(chatId: String, message: String) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    Log.d("ChatViewModel", "🚨 Enviando alerta de emergencia FCM")
                    
                    val location = "${user.latitude}, ${user.longitude}" // Ubicación actual
                    
                    repository.sendEmergencyFCMAlert(
                        message = message,
                        location = location,
                        alertLevel = "high"
                    ).onSuccess { result ->
                        Log.d("ChatViewModel", "✅ Alerta FCM enviada: $result")
                        
                        // También enviar mensaje de emergencia al chat actual
                        sendMessage(chatId, "🚨 ALERTA DE EMERGENCIA: $message", MessageType.SYSTEM)
                        
                    }.onFailure { error ->
                        Log.e("ChatViewModel", "❌ Error enviando alerta FCM", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Error enviando alerta: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error en alerta de emergencia", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

    /**
     * FUNCIÓN NUEVA: Activa el servicio de voz cuando el usuario entra a un chat grupal
     * 🚫 TEMPORALMENTE DESHABILITADO para evitar mensajes automáticos
     */
    fun activateVoiceServiceForGroupChat(chatId: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "🏢 Servicio de voz DESHABILITADO temporalmente")
                
                // Guardar el chat activo en las preferencias
                userPreferences.setCurrentChatId(chatId)
                
                // 🚫 NO iniciar el servicio de voz para evitar mensajes automáticos
                // val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                //     action = VoiceRecognitionService.ACTION_START_LISTENING
                //     putExtra("chat_id", chatId)
                // }
                // 
                // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                //     context.startForegroundService(intent)
                // } else {
                //     context.startService(intent)
                // }
                
                // Actualizar el estado de la UI
                _uiState.value = _uiState.value.copy(
                    isVoiceServiceActive = false, // Cambiado a false
                    voiceServiceStatus = "⏸️ Servicio de voz deshabilitado"
                )
                
                Log.d("ChatViewModel", "✅ Servicio de voz deshabilitado temporalmente")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error configurando servicio de voz", e)
                _uiState.value = _uiState.value.copy(
                    voiceServiceStatus = "❌ Error configurando servicio"
                )
            }
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Desactiva el servicio de voz cuando el usuario sale del chat grupal
     */
    fun deactivateVoiceServiceFromGroupChat() {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "🚪 Desactivando servicio de voz - saliendo de chat grupal")
                
                // Limpiar el chat activo
                userPreferences.setCurrentChatId(null)
                
                // Detener el servicio de voz
                val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                    action = VoiceRecognitionService.ACTION_STOP_LISTENING
                }
                
                context.startService(intent)
                
                // Actualizar el estado de la UI
                _uiState.value = _uiState.value.copy(
                    isVoiceServiceActive = false,
                    voiceServiceStatus = "⏸️ Servicio pausado"
                )
                
                Log.d("ChatViewModel", "✅ Servicio de voz desactivado")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error desactivando servicio de voz", e)
            }
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Activa el servicio de voz para cualquier chat grupal
     */
    fun checkAndActivateVoiceService(chatId: String, isGroupChat: Boolean) {
        if (isGroupChat) {
            Log.d("ChatViewModel", "🏢 Chat grupal detectado - activando servicio de voz")
            activateVoiceServiceForGroupChat(chatId)
        } else {
            Log.d("ChatViewModel", "💬 Chat individual - no se activa servicio de voz")
            deactivateVoiceServiceFromGroupChat()
        }
    }
    
    /**
     * Terminar llamada grupal
     */
    fun endGroupCall(chatId: String, callId: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "📞 Terminando llamada grupal: $callId")
                
                webRTCClient.endCall()
                
                _uiState.value = _uiState.value.copy(
                    isGroupCallActive = false,
                    groupCallType = null,
                    groupCallId = null
                )
                
                // Enviar mensaje al chat informando del fin de llamada
                sendGroupCallMessage(chatId, "📞 Llamada terminada", "ended")
                
                Log.d("ChatViewModel", "✅ Llamada grupal terminada")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error terminando llamada grupal", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error terminando llamada: ${e.message}"
                )
            }
        }
    }
    
    // 🆕 NUEVAS FUNCIONES PARA DETECTAR Y UNIRSE A LLAMADAS ACTIVAS
    
    /**
     * Iniciar monitoreo de llamadas activas en un chat
     */
    private fun startMonitoringActiveCalls(chatId: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "📡 Iniciando monitoreo de llamadas activas en chat: $chatId")
                
                // 🆕 MONITOREO 1: Llamadas activas
                signalingService.listenForActiveCallsInChat(chatId).collect { activeCalls ->
                    _activeCalls.value = activeCalls
                    _hasActiveCall.value = activeCalls.isNotEmpty()
                    
                    Log.d("ChatViewModel", "📞 Llamadas activas detectadas: ${activeCalls.size}")
                    
                    // Si hay una llamada activa y el usuario no está en ella, mostrar opción para unirse
                    if (activeCalls.isNotEmpty()) {
                        val firstCall = activeCalls.first()
                        currentUser.value?.let { user ->
                            val isUserInCall = firstCall.participants.containsKey(user.id)
                            
                            if (!isUserInCall) {
                                Log.d("ChatViewModel", "📞 Usuario puede unirse a llamada: ${firstCall.callId}")
                                _uiState.value = _uiState.value.copy(
                                    hasIncomingCall = true,
                                    incomingCallId = firstCall.callId,
                                    incomingCallType = if (firstCall.callType == WebRTCCallType.VIDEO) "video" else "audio",
                                    incomingCallerName = firstCall.initiatorName
                                )
                            }
                        }
                    } else {
                        // No hay llamadas activas
                        _uiState.value = _uiState.value.copy(
                            hasIncomingCall = false,
                            incomingCallId = null,
                            incomingCallType = null,
                            incomingCallerName = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error monitoreando llamadas activas", e)
            }
        }
        
        // 🆕 MONITOREO 2: Notificaciones de chat
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "📢 Iniciando monitoreo de notificaciones de chat: $chatId")
                
                signalingService.listenForChatNotifications(chatId).collect { notification ->
                    Log.d("ChatViewModel", "📢 Notificación recibida: $notification")
                    
                    val type = notification["type"] as? String
                    val message = notification["message"] as? String
                    
                    when (type) {
                        "video_call_started" -> {
                            Log.d("ChatViewModel", "📹 Notificación de videollamada recibida: $message")
                            // Verificar si hay llamadas activas para unirse
                            checkForActiveCalls(chatId)
                        }
                        "audio_call_started" -> {
                            Log.d("ChatViewModel", "🎤 Notificación de llamada de audio recibida: $message")
                            // Verificar si hay llamadas activas para unirse
                            checkForActiveCalls(chatId)
                        }
                        else -> {
                            Log.d("ChatViewModel", "📢 Notificación general: $message")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error monitoreando notificaciones de chat", e)
            }
        }
    }
    
    /**
     * Unirse a una llamada activa
     */
    fun joinActiveCall(chatId: String, callId: String) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    // Unirse a la llamada WebRTC
                    val success = webRTCClient.joinCall(callId, user.id, user.name)
                    
                    if (success) {
                        // Obtener información de la llamada
                        val callInfo = signalingService.getCallInfo(callId)
                        callInfo?.let { call ->
                            // Actualizar estado de la UI
                            _uiState.value = _uiState.value.copy(
                                isGroupCallActive = true,
                                groupCallType = if (call.callType == WebRTCCallType.VIDEO) "video" else "audio",
                                groupCallId = callId,
                                hasIncomingCall = false,
                                incomingCallId = null,
                                incomingCallType = null,
                                incomingCallerName = null
                            )
                            
                            // Enviar mensaje al chat
                            sendGroupCallMessage(chatId, "📞 ${user.name} se unió a la llamada", "joined")
                            
                            // Abrir la actividad de videollamada
                            val intent = Intent(context, com.example.demoappchat.presentation.chat.VideoCallActivity::class.java).apply {
                                putExtra(com.example.demoappchat.presentation.chat.VideoCallActivity.EXTRA_CHAT_ID, chatId)
                                putExtra(com.example.demoappchat.presentation.chat.VideoCallActivity.EXTRA_PARTICIPANT_NAME, call.initiatorName)
                                putExtra(com.example.demoappchat.presentation.chat.VideoCallActivity.EXTRA_IS_INCOMING_CALL, false)
                                putExtra("call_id", callId)
                                putExtra("call_type", if (call.callType == WebRTCCallType.VIDEO) "video" else "audio")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            
                            context.startActivity(intent)
                        } ?: run {
                            _uiState.value = _uiState.value.copy(
                                error = "Error obteniendo información de la llamada"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Error al unirse a la llamada"
                        )
                    }
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        error = "Usuario no autenticado"
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error uniéndose a llamada activa", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Rechazar llamada entrante
     */
    fun rejectIncomingCall() {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "❌ Rechazando llamada entrante")
                
                // Limpiar estado de llamada entrante
                _uiState.value = _uiState.value.copy(
                    hasIncomingCall = false,
                    incomingCallId = null,
                    incomingCallType = null,
                    incomingCallerName = null
                )
                
                Log.d("ChatViewModel", "✅ Llamada rechazada exitosamente")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error rechazando llamada", e)
            }
        }
    }
    
    /**
     * Enviar mensaje de rechazo de llamada
     */
    fun sendCallRejectionMessage(chatId: String) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    sendGroupCallMessage(chatId, "📞 ${user.name} rechazó la llamada", "rejected")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error enviando mensaje de rechazo", e)
            }
        }
    }
    
    /**
     * Limpiar error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Verificar si hay llamadas activas al cargar el chat
     */
    fun checkForActiveCalls(chatId: String) {
        viewModelScope.launch {
            try {
                val hasActive = signalingService.hasActiveCallInChat(chatId)
                _hasActiveCall.value = hasActive
                
                if (hasActive) {
                    val activeCall = signalingService.getFirstActiveCallInChat(chatId)
                    activeCall?.let { call ->
                        currentUser.value?.let { user ->
                            val isUserInCall = call.participants.containsKey(user.id)
                            
                            if (!isUserInCall) {
                                // Mostrar banner de llamada entrante
                                _uiState.value = _uiState.value.copy(
                                    hasIncomingCall = true,
                                    incomingCallId = call.callId,
                                    incomingCallType = if (call.callType == WebRTCCallType.VIDEO) "video" else "audio",
                                    incomingCallerName = call.initiatorName
                                )
                            } else {
                                // Usuario ya está en la llamada
                                _uiState.value = _uiState.value.copy(
                                    isGroupCallActive = true,
                                    groupCallType = if (call.callType == WebRTCCallType.VIDEO) "video" else "audio",
                                    groupCallId = call.callId
                                )
                            }
                        }
                    }
                } else {
                    // Limpiar estado si no hay llamadas activas
                    _uiState.value = _uiState.value.copy(
                        hasIncomingCall = false,
                        incomingCallId = null,
                        incomingCallType = null,
                        incomingCallerName = null,
                        isGroupCallActive = false,
                        groupCallType = null,
                        groupCallId = null
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error verificando llamadas activas", e)
            }
        }
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val currentChat: ProximityChat? = null,
    val error: String? = null,
    val isVoiceServiceActive: Boolean = false,
    val voiceServiceStatus: String = "⏸️ Servicio pausado",
    val isGroupCallActive: Boolean = false,
    val groupCallType: String? = null,
    val groupCallId: String? = null,
    // 🆕 Estados para llamadas entrantes
    val hasIncomingCall: Boolean = false,
    val incomingCallId: String? = null,
    val incomingCallType: String? = null,
    val incomingCallerName: String? = null
)