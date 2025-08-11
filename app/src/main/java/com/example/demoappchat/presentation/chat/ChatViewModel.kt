package com.example.demoappchat.presentation.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    
    // Estados para llamadas grupales
    private val _isGroupCallActive = MutableStateFlow(false)
    val isGroupCallActive: StateFlow<Boolean> = _isGroupCallActive.asStateFlow()
    
    private val _groupCallId = MutableStateFlow<String?>(null)
    val groupCallId: StateFlow<String?> = _groupCallId.asStateFlow()

    fun loadChat(chatId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                loadMessages(chatId)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error cargando chat: ${e.message}"
                )
            }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                repository.getChatMessages(chatId).collect { messagesList ->
                    _messages.value = messagesList
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error cargando mensajes: ${e.message}"
                )
            }
        }
    }

    fun sendMessage(chatId: String, content: String, type: MessageType = MessageType.TEXT) {
        if (content.trim().isEmpty()) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                currentUser.value?.let { user ->
                    val message = ChatMessage(
                        chatId = chatId,
                        userId = user.id,
                        userName = user.name,
                        userPhotoUrl = user.photoUrl,
                        messageType = type,
                        content = content.trim(),
                        timestamp = System.currentTimeMillis()
                    )

                    val result = repository.sendMessage(message)
                    if (result.isSuccess) {
                        Log.d("ChatViewModel", "✅ Mensaje enviado exitosamente")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    } else {
                        throw Exception(result.exceptionOrNull()?.message ?: "Error desconocido")
                    }
                } ?: throw Exception("Usuario no autenticado")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error enviando mensaje", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error enviando mensaje: ${e.message}"
                )
            }
        }
    }

    fun sendMediaMessage(chatId: String, uri: Uri, type: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Subir archivo primero
                val file = createTempFileFromUri(uri, context)
                val mediaUrl = repository.uploadMediaFile(file, type, chatId)
                
                // Enviar mensaje con media
                repository.sendMediaMessage(chatId, mediaUrl, type, "")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error enviando media", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error enviando media: ${e.message}"
                )
            }
        }
    }

    // Función simplificada para iniciar llamada grupal
    fun startGroupCall(chatId: String, callType: String = "VIDEO") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                currentUser.value?.let { user ->
                    val result = repository.startGroupCallWithNotifications(
                        chatId = chatId,
                        callType = callType,
                        callerName = user.name
                    )
                    
                    if (result.isSuccess) {
                        _isGroupCallActive.value = true
                        _groupCallId.value = "${System.currentTimeMillis()}_$chatId"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        Log.d("ChatViewModel", "✅ Llamada grupal iniciada")
                    } else {
                        throw Exception(result.exceptionOrNull()?.message ?: "Error iniciando llamada")
                    }
                } ?: throw Exception("Usuario no autenticado")
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error iniciando llamada grupal", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error iniciando llamada: ${e.message}"
                )
            }
        }
    }

    // Función para terminar llamada grupal
    fun endGroupCall() {
        _isGroupCallActive.value = false
        _groupCallId.value = null
        Log.d("ChatViewModel", "📞 Llamada grupal terminada")
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun createTempFileFromUri(uri: Uri, context: Context): java.io.File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = java.io.File.createTempFile("media_", ".tmp", context.cacheDir)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar recursos si es necesario
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)