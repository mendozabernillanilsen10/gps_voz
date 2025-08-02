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
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.service.VoiceRecognitionService
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val currentUser = repository.currentUser

    fun loadChat(chatId: String) {
        viewModelScope.launch {
            // Cargar información del chat
            // Implementar método en repository para obtener chat específico
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
                            "photo" -> ".jpg"
                            else -> ".mp4"
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
                    messageType = type.uppercase(),
                    content = when (type) {
                        "audio" -> "Audio enviado"
                        "video" -> "Video enviado"
                        "photo" -> "Foto enviada"
                        else -> "Archivo enviado"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
                    Log.d("ChatViewModel", "📹 Iniciando videollamada grupal en chat: $chatId")
                    
                    // Enviar notificaciones FCM a todos los participantes
                    repository.sendGroupCallNotification(
                        chatId = chatId,
                        callType = "video_call",
                        callerName = user.name
                    ).onSuccess { result ->
                        Log.d("ChatViewModel", "✅ Notificaciones FCM enviadas: $result")
                        
                        // Enviar mensaje al chat informando del inicio de llamada
                        sendGroupCallMessage(chatId, "📹 Videollamada iniciada", "video")
                        
                    }.onFailure { error ->
                        Log.e("ChatViewModel", "❌ Error enviando notificaciones FCM", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Error iniciando videollamada: ${error.message}"
                        )
                    }
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
                    Log.d("ChatViewModel", "🎤 Iniciando llamada de audio grupal en chat: $chatId")
                    
                    // Enviar notificaciones FCM a todos los participantes
                    repository.sendGroupCallNotification(
                        chatId = chatId,
                        callType = "audio_call",
                        callerName = user.name
                    ).onSuccess { result ->
                        Log.d("ChatViewModel", "✅ Notificaciones FCM enviadas: $result")
                        
                        // Enviar mensaje al chat informando del inicio de llamada
                        sendGroupCallMessage(chatId, "🎤 Llamada de audio iniciada", "audio")
                        
                    }.onFailure { error ->
                        Log.e("ChatViewModel", "❌ Error enviando notificaciones FCM", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Error iniciando llamada de audio: ${error.message}"
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
     */
    fun activateVoiceServiceForGroupChat(chatId: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "🏢 Activando servicio de voz para chat grupal: $chatId")
                
                // Guardar el chat activo en las preferencias
                userPreferences.setCurrentChatId(chatId)
                
                // Iniciar el servicio de voz en segundo plano
                val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                    action = VoiceRecognitionService.ACTION_START_LISTENING
                    putExtra("chat_id", chatId)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                
                // Actualizar el estado de la UI
                _uiState.value = _uiState.value.copy(
                    isVoiceServiceActive = true,
                    voiceServiceStatus = "🎤 Escuchando comandos de voz"
                )
                
                Log.d("ChatViewModel", "✅ Servicio de voz activado para chat grupal")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error activando servicio de voz", e)
                _uiState.value = _uiState.value.copy(
                    voiceServiceStatus = "❌ Error activando servicio"
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
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val currentChat: ProximityChat? = null,
    val error: String? = null,
    val isVoiceServiceActive: Boolean = false,
    val voiceServiceStatus: String = "⏸️ Servicio pausado"
)