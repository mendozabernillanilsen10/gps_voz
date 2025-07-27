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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val userPreferences: UserPreferences,
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

    fun updateLocation(location: Location) {
        _currentLocation.value = location

        viewModelScope.launch {
            repository.updateUserLocation(location.latitude, location.longitude)
            repository.startListeningToNearbyChats(location.latitude, location.longitude)
        }
    }

    fun createChat(chat: ProximityChat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.createProximityChat(chat)
                .onSuccess { chatId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        createdChatId = chatId
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun joinChat(chatId: String, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
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
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNavigationEvents() {
        _uiState.value = _uiState.value.copy(
            createdChatId = null,
            joinedChatId = null
        )
    }

    fun toggleVoiceService(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVoiceServiceEnabled(enabled)
            
            Log.d("MainViewModel", "Toggle voice service: $enabled")
            
            if (enabled) {
                startVoiceService()
            } else {
                stopVoiceService()
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
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null,
    val joinedChatId: String? = null
)