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
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val currentChat: ProximityChat? = null,
    val error: String? = null
)