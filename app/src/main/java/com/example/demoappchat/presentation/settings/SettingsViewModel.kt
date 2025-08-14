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
    ),
    // NUEVAS CONFIGURACIONES DE VOZ
    val voiceSensitivity: Float = 0.7f,
    val stealthMode: Boolean = false,
    val voiceDetectionEnabled: Boolean = true,
    val continuousListening: Boolean = true
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
                userPreferences.getCommandActions(),
                userPreferences.getVoiceSensitivity(),
                userPreferences.getStealthMode(),
                userPreferences.getVoiceDetectionEnabled(),
                userPreferences.getContinuousListening()
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
                val sensitivity = flows[14] as Float
                val stealth = flows[15] as Boolean
                val detection = flows[16] as Boolean
                val continuous = flows[17] as Boolean
                
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
                    commandActions = commandActions,
                    voiceSensitivity = sensitivity,
                    stealthMode = stealth,
                    voiceDetectionEnabled = detection,
                    continuousListening = continuous
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        
        // Cargar configuraciones adicionales desde SharedPreferences
        loadAllVoiceSettings()
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
    
    // NUEVOS MÉTODOS PARA CONFIGURACIÓN AVANZADA DE VOZ
    fun setVoiceSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            userPreferences.setVoiceSensitivity(sensitivity)
            _uiState.value = _uiState.value.copy(voiceSensitivity = sensitivity)
            
            // Sincronizar con el servicio de voz
            saveVoiceSettingsToSharedPreferences()
            Log.d("SettingsViewModel", "🎚️ Sensibilidad ajustada: $sensitivity")
        }
    }
    
    fun toggleStealthMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setStealthMode(enabled)
            _uiState.value = _uiState.value.copy(stealthMode = enabled)
            
            // Sincronizar con el servicio de voz
            saveVoiceSettingsToSharedPreferences()
            Log.d("SettingsViewModel", "🔇 Modo sigiloso: ${if (enabled) "activado" else "desactivado"}")
        }
    }
    
    fun toggleVoiceDetection(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVoiceDetectionEnabled(enabled)
            _uiState.value = _uiState.value.copy(voiceDetectionEnabled = enabled)
            
            // Sincronizar con el servicio de voz
            saveVoiceSettingsToSharedPreferences()
            Log.d("SettingsViewModel", "🎤 Detección de voz: ${if (enabled) "activada" else "desactivada"}")
        }
    }
    
    fun toggleContinuousListening(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setContinuousListening(enabled)
            _uiState.value = _uiState.value.copy(continuousListening = enabled)
            
            // Sincronizar con el servicio de voz
            saveVoiceSettingsToSharedPreferences()
            Log.d("SettingsViewModel", "🔄 Escucha continua: ${if (enabled) "activada" else "desactivada"}")
        }
    }
    
    // MÉTODO PARA CARGAR TODAS LAS CONFIGURACIONES DE VOZ
    fun loadAllVoiceSettings() {
        viewModelScope.launch {
            try {
                val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
                
                // Cargar sensibilidad
                val sensitivity = sharedPrefs.getFloat("voice_sensitivity", 0.7f)
                
                // Cargar modo sigiloso
                val stealth = sharedPrefs.getBoolean("stealth_mode", false)
                
                // Cargar detección de voz
                val detection = sharedPrefs.getBoolean("voice_detection_enabled", true)
                
                // Cargar escucha continua
                val continuous = sharedPrefs.getBoolean("continuous_listening", true)
                
                // Cargar comandos personalizados
                val commandActionsString = sharedPrefs.getString("command_actions", "")
                val commandActions = if (commandActionsString.isNullOrEmpty()) {
                    mapOf<String, String>()
                } else {
                    commandActionsString.split(",").associate { pair ->
                        val parts = pair.split(":")
                        if (parts.size == 2) parts[0] to parts[1] else "" to ""
                    }.filter { it.key.isNotEmpty() }
                }
                
                _uiState.value = _uiState.value.copy(
                    voiceSensitivity = sensitivity,
                    stealthMode = stealth,
                    voiceDetectionEnabled = detection,
                    continuousListening = continuous,
                    commandActions = commandActions
                )
                
                Log.d("SettingsViewModel", "📥 Configuraciones de voz cargadas - Sensibilidad: $sensitivity, Sigiloso: $stealth, Detección: $detection, Continua: $continuous, Comandos: ${commandActions.size}")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error cargando configuraciones de voz: ${e.message}")
            }
        }
    }
    
    // MÉTODO PARA REINICIAR CONFIGURACIONES DE VOZ
    fun resetVoiceSettings() {
        viewModelScope.launch {
            try {
                val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
                
                _uiState.value = _uiState.value.copy(
                    voiceSensitivity = 0.7f,
                    stealthMode = false,
                    voiceDetectionEnabled = true,
                    continuousListening = true,
                    commandActions = mapOf(
                        "grabar audio" to "AUDIO",
                        "grabar video" to "VIDEO",
                        "foto" to "PHOTO",
                        "emergencia" to "AUDIO",
                        "alerta" to "AUDIO"
                    )
                )
                
                // Guardar configuraciones por defecto
                saveVoiceSettingsToSharedPreferences()
                saveCommandActionsToSharedPreferences(_uiState.value.commandActions)
                
                Log.d("SettingsViewModel", "🔄 Configuraciones de voz reiniciadas")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error reiniciando configuraciones: ${e.message}")
            }
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
    
    private fun saveVoiceSettingsToSharedPreferences() {
        try {
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putFloat("voice_sensitivity", _uiState.value.voiceSensitivity)
                .putBoolean("stealth_mode", _uiState.value.stealthMode)
                .putBoolean("voice_detection_enabled", _uiState.value.voiceDetectionEnabled)
                .putBoolean("continuous_listening", _uiState.value.continuousListening)
                .apply()
            
            Log.d("SettingsViewModel", "⚙️ Configuraciones de voz sincronizadas - Sensibilidad: ${_uiState.value.voiceSensitivity}, Sigiloso: ${_uiState.value.stealthMode}")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "❌ Error sincronizando configuraciones de voz: ${e.message}")
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