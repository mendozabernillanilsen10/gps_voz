package com.example.demoappchat.presentation.main


import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.service.VoiceRecognitionService
import com.example.demoappchat.data.service.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val userPreferences: UserPreferences,
    private val errorLogger: ErrorLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val nearbyChats: StateFlow<List<ProximityChat>> = repository.nearbyChats
    val currentUser = repository.currentUser

    // Estado del servicio de voz
    val isVoiceServiceEnabled: StateFlow<Boolean> = userPreferences.getVoiceServiceEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Debounce para evitar múltiples actualizaciones de ubicación
    private var locationUpdateJob: kotlinx.coroutines.Job? = null

    fun updateLocation(location: Location) {
        Log.d("MainViewModel", "📍 Actualizando ubicación: ${location.latitude}, ${location.longitude}")
        _currentLocation.value = location

        // Cancelar actualización anterior si existe
        locationUpdateJob?.cancel()
        
        // Debounce de 1 segundo para evitar múltiples actualizaciones
        locationUpdateJob = viewModelScope.launch {
            delay(1000) // Esperar 1 segundo antes de actualizar
            
            try {
                Log.d("MainViewModel", "🔄 Actualizando ubicación en Firebase")
                repository.updateUserLocation(location.latitude, location.longitude)
                
                Log.d("MainViewModel", "🎧 Iniciando escucha de chats cercanos")
                repository.startListeningToNearbyChats(location.latitude, location.longitude)
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error actualizando ubicación", e)
            }
        }
    }

    fun createChat(chat: ProximityChat) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "🔄 Iniciando creación de chat: ${chat.title}")
                
                // Log específico para Honor devices
               // if (ErrorLogger.isHonorDevice()) {
                    errorLogger.logHonorSpecificIssue(
                        issue = "chat_creation_start",
                        context = "MainViewModel.createChat",
                        additionalData = mapOf(
                            "chat_title" to chat.title,
                            "chat_category" to chat.category,
                            "pin_length" to chat.pin.length
                        )
                    )
               // }
                
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Validar datos del chat antes de enviarlo
                if (chat.title.isBlank() || chat.description.isBlank() || chat.pin.length != 4) {
                    val error = Exception("Datos del chat incompletos")
                    errorLogger.logChatCreationError(
                        chatTitle = chat.title,
                        step = "validation",
                        throwable = error,
                        additionalData = mapOf(
                            "title_blank" to chat.title.isBlank(),
                            "description_blank" to chat.description.isBlank(),
                            "pin_length" to chat.pin.length
                        )
                    )
                    throw error
                }

                Log.d("MainViewModel", "📝 Datos del chat validados correctamente")
                
                repository.createProximityChat(chat)
                    .onSuccess { chatId: String ->
                        Log.d("MainViewModel", "✅ Chat creado exitosamente con ID: $chatId")
                        
                        // Log de éxito para Honor
                        //if (ErrorLogger.isHonorDevice()) {
                            errorLogger.logHonorSpecificIssue(
                                issue = "chat_creation_success",
                                context = "MainViewModel.createChat",
                                additionalData = mapOf(
                                    "chat_id" to chatId,
                                    "chat_title" to chat.title
                                )
                            )
                       // }
                        
                        // Esperar un momento antes de actualizar el estado
                        kotlinx.coroutines.delay(100)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            createdChatId = chatId,
                            error = null
                        )
                        
                        Log.d("MainViewModel", "🎯 Estado actualizado para navegación con chatId: $chatId")
                        
                        // Log adicional para Honor después de actualizar estado
                        //if (ErrorLogger.isHonorDevice()) {
                            errorLogger.logHonorSpecificIssue(
                                issue = "state_updated_for_navigation",
                                context = "MainViewModel.createChat",
                                additionalData = mapOf(
                                    "chat_id" to chatId,
                                    "ui_state_chat_id" to (_uiState.value.createdChatId ?: "null"),
                                    "ui_state_loading" to _uiState.value.isLoading
                                )
                            )
                        //}
                    }
                    .onFailure { exception ->
                        Log.e("MainViewModel", "❌ Error en repositorio creando chat", exception)
                        
                        errorLogger.logChatCreationError(
                            chatTitle = chat.title,
                            step = "repository_creation",
                            throwable = exception,
                            additionalData = mapOf(
                                "exception_type" to (exception::class.simpleName ?: "Unknown"),
                                "exception_message" to (exception.message ?: "No message")
                            )
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Error desconocido creando chat"
                        )
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error general creando chat", e)
                
                errorLogger.logChatCreationError(
                    chatTitle = chat.title,
                    step = "general_error",
                    throwable = e,
                    additionalData = mapOf(
                        "exception_type" to (e::class.simpleName ?: "Unknown"),
                        "exception_message" to (e.message ?: "No message")
                    )
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun joinChat(chatId: String, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.joinChatWithPin(chatId, pin)
                    .onSuccess { success ->
                        onResult(success)
                        if (success) {
                            _uiState.value = _uiState.value.copy(joinedChatId = chatId)
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(error = exception.message)
                        onResult(false)
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error uniéndose al chat", e)
                _uiState.value = _uiState.value.copy(error = e.message)
                onResult(false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNavigationEvents() {
        Log.d("MainViewModel", "🧹 Limpiando eventos de navegación")
        _uiState.value = _uiState.value.copy(
            createdChatId = null,
            joinedChatId = null,
            error = null
        )
    }

    fun toggleVoiceService(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferences.setVoiceServiceEnabled(enabled)
                
                Log.d("MainViewModel", "Toggle voice service: $enabled")
                
                if (enabled) {
                    startVoiceService()
                } else {
                    stopVoiceService()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error toggling voice service", e)
            }
        }
    }
    
    private fun startVoiceService() {
        try {
            val intent = Intent(context, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_START_LISTENING
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d("MainViewModel", "Voice service started")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error starting voice service", e)
        }
    }
    
    private fun stopVoiceService() {
        try {
            val intent = Intent(context, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_STOP_LISTENING
            context.startService(intent)
            Log.d("MainViewModel", "Voice service stopped")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping voice service", e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Limpiar recursos al destruir el ViewModel
        locationUpdateJob?.cancel()
        repository.stopListeningToNearbyChats()
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null,
    val joinedChatId: String? = null
)