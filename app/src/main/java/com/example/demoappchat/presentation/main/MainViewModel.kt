package com.example.demoappchat.presentation.main


import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.data.repository.FirebaseRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val nearbyChats: StateFlow<List<ProximityChat>> = repository.nearbyChats
    val currentUser = repository.currentUser

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
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null,
    val joinedChatId: String? = null
)