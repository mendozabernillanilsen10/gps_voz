package com.example.demoappchat.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordingState(
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val recordingType: String = "AUDIO",
    val recordingTime: Int = 0,
    val lastCommand: String? = null,
    val voiceServiceEnabled: Boolean = false
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingTimer = MutableStateFlow(0)
    val recordingTimer: StateFlow<Int> = _recordingTimer.asStateFlow()

    init {
        // Observar el estado del servicio de voz
        viewModelScope.launch {
            userPreferences.getVoiceServiceEnabled()
                .collect { enabled ->
                    _recordingState.value = _recordingState.value.copy(
                        voiceServiceEnabled = enabled,
                        isListening = enabled
                    )
                }
        }
    }

    fun startRecording(type: String = "AUDIO") {
        viewModelScope.launch {
            _recordingState.value = _recordingState.value.copy(
                isRecording = true,
                recordingType = type,
                recordingTime = 0
            )
            
            // Iniciar temporizador
            startRecordingTimer()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            _recordingState.value = _recordingState.value.copy(
                isRecording = false,
                recordingTime = 0
            )
            
            _recordingTimer.value = 0
        }
    }

    fun updateRecordingTime(time: Int) {
        _recordingState.value = _recordingState.value.copy(recordingTime = time)
        _recordingTimer.value = time
    }

    fun setCommandDetected(command: String) {
        _recordingState.value = _recordingState.value.copy(lastCommand = command)
    }

    fun setVoiceServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVoiceServiceEnabled(enabled)
        }
    }

    private fun startRecordingTimer() {
        viewModelScope.launch {
            var time = 0
            while (_recordingState.value.isRecording) {
                kotlinx.coroutines.delay(1000L)
                time++
                updateRecordingTime(time)
            }
        }
    }
}