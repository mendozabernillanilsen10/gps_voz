package com.example.demoappchat.presentation.settings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.service.BackgroundVoiceService
import com.example.demoappchat.domain.usecase.voice.StartBackgroundVoiceServiceUseCase
import com.example.demoappchat.domain.usecase.voice.StopBackgroundVoiceServiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackgroundVoiceSettingsViewModel @Inject constructor(
    private val startBackgroundVoiceServiceUseCase: StartBackgroundVoiceServiceUseCase,
    private val stopBackgroundVoiceServiceUseCase: StopBackgroundVoiceServiceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackgroundVoiceSettingsUiState())
    val uiState: StateFlow<BackgroundVoiceSettingsUiState> = _uiState

    init {
        checkServiceStatus()
    }

    fun startBackgroundService() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                startBackgroundVoiceServiceUseCase.execute()
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = true,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error iniciando servicio"
                )
            }
        }
    }

    fun stopBackgroundService() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                stopBackgroundVoiceServiceUseCase.execute()
                _uiState.value = _uiState.value.copy(
                    isServiceRunning = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error deteniendo servicio"
                )
            }
        }
    }

    fun checkServiceStatus() {
        // Esta función se llamaría desde el contexto de la actividad
        // Por ahora la dejamos vacía
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BackgroundVoiceSettingsUiState(
    val isServiceRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
