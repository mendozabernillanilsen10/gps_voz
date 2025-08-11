package com.example.demoappchat.presentation.settings

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.service.VoiceRecognitionService

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isVoiceServiceEnabled: Boolean = false,
    val discreteMode: Boolean = true,
    val voiceCommands: List<String> = listOf("óyeme", "alerta", "grabar video", "ayuda"),
    val transmissionRadius: Float = 4.0f, // km
    val audioQuality: String = "Media",
    val autoUploadRecordings: Boolean = true,
    val emergencyContacts: List<String> = emptyList(),
    val shareLocation: Boolean = true,
    val dataRetentionDays: Int = 7,
    // NUEVAS PREFERENCIAS DE GRABACIÓN
    val audioRecordingDuration: Int = 30,
    val videoRecordingDuration: Int = 15,
    val photoCaptureEnabled: Boolean = true,
    val autoSendRecordings: Boolean = true,
    val recordingQuality: String = "HIGH",
    val commandActions: Map<String, String> = mapOf(
        "grabar audio" to "AUDIO",
        "grabar video" to "VIDEO",
        "foto" to "PHOTO",
        "emergencia" to "AUDIO",
        "alerta" to "AUDIO"
    )
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferences.getVoiceServiceEnabled(),
                userPreferences.getDiscreteMode(),
                userPreferences.getVoiceCommands(),
                userPreferences.getTransmissionRadius(),
                userPreferences.getAudioQuality(),
                userPreferences.getAutoUploadRecordings(),
                userPreferences.getShareLocation(),
                userPreferences.getDataRetentionDays(),
                userPreferences.getAudioRecordingDuration(),
                userPreferences.getVideoRecordingDuration(),
                userPreferences.getPhotoCaptureEnabled(),
                userPreferences.getAutoSendRecordings(),
                userPreferences.getRecordingQuality(),
                userPreferences.getCommandActions()
            ) { flows ->
                val enabled = flows[0] as Boolean
                val discrete = flows[1] as Boolean  
                val commands = flows[2] as List<String>
                val radius = flows[3] as Float
                val quality = flows[4] as String
                val autoUpload = flows[5] as Boolean
                val shareLocation = flows[6] as Boolean
                val retention = flows[7] as Int
                val audioDuration = flows[8] as Int
                val videoDuration = flows[9] as Int
                val photoCapture = flows[10] as Boolean
                val autoSend = flows[11] as Boolean
                val recordingQuality = flows[12] as String
                val commandActions = flows[13] as Map<String, String>
                
                SettingsUiState(
                    isVoiceServiceEnabled = enabled,
                    discreteMode = discrete,
                    voiceCommands = commands,
                    transmissionRadius = radius,
                    audioQuality = quality,
                    autoUploadRecordings = autoUpload,
                    shareLocation = shareLocation,
                    dataRetentionDays = retention,
                    audioRecordingDuration = audioDuration,
                    videoRecordingDuration = videoDuration,
                    photoCaptureEnabled = photoCapture,
                    autoSendRecordings = autoSend,
                    recordingQuality = recordingQuality,
                    commandActions = commandActions
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun toggleVoiceService(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVoiceServiceEnabled(enabled)
            _uiState.value = _uiState.value.copy(isVoiceServiceEnabled = enabled)
            
            // Actually start or stop the voice service
            if (enabled) {
                startVoiceService()
            } else {
                stopVoiceService()
            }
        }
    }
    
    private fun startVoiceService() {
        try {
            Log.d("SettingsViewModel", "Iniciando servicio de voz...")
            val intent = Intent(context, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_START_LISTENING
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error starting voice service", e)
        }
    }
    
    private fun stopVoiceService() {
        try {
            val intent = Intent(context, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_STOP_LISTENING
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun toggleDiscreteMode(discrete: Boolean) {
        viewModelScope.launch {
            userPreferences.setDiscreteMode(discrete)
            _uiState.value = _uiState.value.copy(discreteMode = discrete)
        }
    }
    
    fun addVoiceCommand(command: String) {
        val currentCommands = _uiState.value.voiceCommands
        if (!currentCommands.contains(command)) {
            val newCommands = currentCommands + command
            viewModelScope.launch {
                userPreferences.setVoiceCommands(newCommands)
                _uiState.value = _uiState.value.copy(voiceCommands = newCommands)
            }
        }
    }
    
    fun removeVoiceCommand(command: String) {
        val currentCommands = _uiState.value.voiceCommands
        val newCommands = currentCommands.filter { it != command }
        viewModelScope.launch {
            userPreferences.setVoiceCommands(newCommands)
            _uiState.value = _uiState.value.copy(voiceCommands = newCommands)
        }
    }
    
    fun updateTransmissionRadius(radius: Float) {
        viewModelScope.launch {
            userPreferences.setTransmissionRadius(radius)
            _uiState.value = _uiState.value.copy(transmissionRadius = radius)
        }
    }
    
    fun updateAudioQuality(quality: String) {
        viewModelScope.launch {
            userPreferences.setAudioQuality(quality)
            _uiState.value = _uiState.value.copy(audioQuality = quality)
        }
    }
    
    fun toggleAutoUpload(autoUpload: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoUploadRecordings(autoUpload)
            _uiState.value = _uiState.value.copy(autoUploadRecordings = autoUpload)
        }
    }
    
    fun updateEmergencyContacts(contacts: List<String>) {
        viewModelScope.launch {
            // Save emergency contacts to preferences
            _uiState.value = _uiState.value.copy(emergencyContacts = contacts)
        }
    }
    
    // NUEVOS MÉTODOS PARA CONFIGURACIÓN DE GRABACIÓN
    fun setAudioRecordingDuration(duration: Int) {
        viewModelScope.launch {
            userPreferences.setAudioRecordingDuration(duration)
            _uiState.value = _uiState.value.copy(audioRecordingDuration = duration)
            
            // Sincronizar con el servicio de voz
            saveRecordingSettingsToSharedPreferences()
        }
    }
    
    fun setVideoRecordingDuration(duration: Int) {
        viewModelScope.launch {
            userPreferences.setVideoRecordingDuration(duration)
            _uiState.value = _uiState.value.copy(videoRecordingDuration = duration)
            
            // Sincronizar con el servicio de voz
            saveRecordingSettingsToSharedPreferences()
        }
    }
    
    fun setPhotoCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setPhotoCaptureEnabled(enabled)
            _uiState.value = _uiState.value.copy(photoCaptureEnabled = enabled)
        }
    }
    
    fun setAutoSendRecordings(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoSendRecordings(enabled)
            _uiState.value = _uiState.value.copy(autoSendRecordings = enabled)
        }
    }
    
    fun setRecordingQuality(quality: String) {
        viewModelScope.launch {
            userPreferences.setRecordingQuality(quality)
            _uiState.value = _uiState.value.copy(recordingQuality = quality)
            
            // Sincronizar con el servicio de voz
            saveRecordingSettingsToSharedPreferences()
        }
    }
    
    fun setCommandAction(command: String, action: String) {
        viewModelScope.launch {
            val currentActions = _uiState.value.commandActions.toMutableMap()
            currentActions[command] = action
            userPreferences.setCommandActions(currentActions)
            _uiState.value = _uiState.value.copy(commandActions = currentActions)
            
            // También guardar en SharedPreferences para el servicio de voz
            saveCommandActionsToSharedPreferences(currentActions)
            Log.d("SettingsViewModel", "💾 Comando guardado: '$command' -> $action")
        }
    }
    
    fun removeCommandAction(command: String) {
        viewModelScope.launch {
            val currentActions = _uiState.value.commandActions.toMutableMap()
            currentActions.remove(command)
            userPreferences.setCommandActions(currentActions)
            _uiState.value = _uiState.value.copy(commandActions = currentActions)
            
            // También guardar en SharedPreferences para el servicio de voz
            saveCommandActionsToSharedPreferences(currentActions)
            Log.d("SettingsViewModel", "🗑️ Comando removido: '$command'")
        }
    }
    
    private fun saveCommandActionsToSharedPreferences(actions: Map<String, String>) {
        try {
            val actionsString = actions.map { "${it.key}:${it.value}" }.joinToString(",")
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("command_actions", actionsString).apply()
            Log.d("SettingsViewModel", "💾 Comandos sincronizados con servicio: $actionsString")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "❌ Error sincronizando comandos: ${e.message}")
        }
    }
    
    private fun saveRecordingSettingsToSharedPreferences() {
        try {
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putInt("audio_recording_duration", _uiState.value.audioRecordingDuration)
                .putInt("video_recording_duration", _uiState.value.videoRecordingDuration)
                .putString("recording_quality", _uiState.value.recordingQuality)
                .apply()
            
            Log.d("SettingsViewModel", "⚙️ Configuraciones de grabación sincronizadas - Audio: ${_uiState.value.audioRecordingDuration}s, Video: ${_uiState.value.videoRecordingDuration}s, Calidad: ${_uiState.value.recordingQuality}")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "❌ Error sincronizando configuraciones: ${e.message}")
        }
    }
    
    fun toggleShareLocation(share: Boolean) {
        viewModelScope.launch {
            userPreferences.setShareLocation(share)
            _uiState.value = _uiState.value.copy(shareLocation = share)
        }
    }
    
    fun updateDataRetention(days: Int) {
        viewModelScope.launch {
            userPreferences.setDataRetentionDays(days)
            _uiState.value = _uiState.value.copy(dataRetentionDays = days)
        }
    }
}