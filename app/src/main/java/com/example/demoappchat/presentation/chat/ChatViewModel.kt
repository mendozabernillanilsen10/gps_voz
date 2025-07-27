package com.example.demoappchat.presentation.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val userPreferences: UserPreferences
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
            userPreferences.setCurrentChatId(chatId)
        }
    }
    
    fun clearCurrentChatId() {
        viewModelScope.launch {
            userPreferences.setCurrentChatId(null)
        }
    }
    
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
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val currentChat: ProximityChat? = null,
    val error: String? = null
)