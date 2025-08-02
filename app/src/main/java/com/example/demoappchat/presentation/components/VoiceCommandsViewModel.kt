package com.example.demoappchat.presentation.components

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.local.preferences.VoicePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceCommandsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // private val voicePreferences: VoicePreferences
) : ViewModel() {

    private val _commandActions = MutableStateFlow<Map<String, String>>(emptyMap())
    val commandActions: StateFlow<Map<String, String>> = _commandActions.asStateFlow()

    init {
        loadCommandActions()
    }

    private fun loadCommandActions() {
        viewModelScope.launch {
            // Cargar desde SharedPreferences para sincronizar con el servicio
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            val actionsString = sharedPrefs.getString("command_actions", null)
            
            if (actionsString != null && actionsString.isNotEmpty()) {
                try {
                    val actions = actionsString.split(",").associate { action ->
                        val parts = action.split(":")
                        if (parts.size == 2) parts[0] to parts[1] else "" to ""
                    }.filter { it.key.isNotEmpty() }
                    _commandActions.value = actions
                    Log.d("VoiceCommandsVM", "✅ Comandos cargados desde SharedPreferences: $actions")
                } catch (e: Exception) {
                    Log.e("VoiceCommandsVM", "❌ Error parseando comandos: ${e.message}")
                    _commandActions.value = getDefaultCommandActions()
                }
            } else {
                // Usar comandos por defecto
                _commandActions.value = getDefaultCommandActions()
                // Guardar comandos por defecto
                saveCommandActionsToSharedPreferences(getDefaultCommandActions())
            }
        }
    }
    
    private fun saveCommandActionsToSharedPreferences(actions: Map<String, String>) {
        try {
            val actionsString = actions.map { "${it.key}:${it.value}" }.joinToString(",")
            val sharedPrefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("command_actions", actionsString).apply()
            Log.d("VoiceCommandsVM", "💾 Comandos guardados en SharedPreferences: $actionsString")
        } catch (e: Exception) {
            Log.e("VoiceCommandsVM", "❌ Error guardando comandos: ${e.message}")
        }
    }
    
    private fun getDefaultCommandActions(): Map<String, String> {
        return mapOf(
            "óyeme" to "AUDIO",
            "alerta" to "TEXT",
            "grabar video" to "VIDEO",
            "ayuda" to "LOCATION",
            "foto" to "PHOTO",
            "emergencia" to "CALL"
        )
    }

    fun addCommand(command: String, action: String) {
        viewModelScope.launch {
            val currentActions = _commandActions.value.toMutableMap()
            currentActions[command] = action
            _commandActions.value = currentActions
            saveCommandActionsToSharedPreferences(currentActions)
            Log.d("VoiceCommandsVM", "➕ Comando agregado: '$command' -> $action")
        }
    }

    fun removeCommand(command: String) {
        viewModelScope.launch {
            val currentActions = _commandActions.value.toMutableMap()
            currentActions.remove(command)
            _commandActions.value = currentActions
            saveCommandActionsToSharedPreferences(currentActions)
            Log.d("VoiceCommandsVM", "➖ Comando removido: '$command'")
        }
    }

    fun updateCommandAction(command: String, action: String) {
        viewModelScope.launch {
            val currentActions = _commandActions.value.toMutableMap()
            currentActions[command] = action
            _commandActions.value = currentActions
            saveCommandActionsToSharedPreferences(currentActions)
            Log.d("VoiceCommandsVM", "✏️ Comando actualizado: '$command' -> $action")
        }
    }
} 