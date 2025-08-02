package com.example.demoappchat.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.demoappchat.domain.model.VoiceAction
import com.example.demoappchat.domain.model.VoiceCommand
import com.example.demoappchat.domain.model.CommandPriority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestión profesional de preferencias de voz
 * Persistencia de comandos, configuración y estado del sistema
 */
@Singleton
class VoicePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "voice_preferences", Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_VOICE_COMMANDS = "voice_commands"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_PREFERRED_ENGINE = "preferred_engine"
        private const val KEY_AUTO_FALLBACK = "auto_fallback"
        private const val KEY_BATTERY_OPTIMIZATION = "battery_optimization"
        private const val KEY_WAKE_PHRASE = "wake_phrase"
        private const val KEY_WAKE_PHRASE_ENABLED = "wake_phrase_enabled"
        private const val KEY_CURRENT_CHAT_ID = "current_chat_id"
        private const val KEY_24X7_MODE_ENABLED = "24x7_mode_enabled"
        private const val KEY_LAST_ACTIVATION_TIME = "last_activation_time"
        
        // NUEVAS PREFERENCIAS DE GRABACIÓN
        private const val KEY_AUDIO_RECORDING_DURATION = "audio_recording_duration"
        private const val KEY_VIDEO_RECORDING_DURATION = "video_recording_duration"
        private const val KEY_PHOTO_CAPTURE_ENABLED = "photo_capture_enabled"
        private const val KEY_AUTO_SEND_RECORDINGS = "auto_send_recordings"
        private const val KEY_RECORDING_QUALITY = "recording_quality"
        private const val KEY_COMMAND_ACTIONS = "command_actions"
        
        // Comandos por defecto para operaciones policiales
        private val DEFAULT_COMMANDS = listOf(
            VoiceCommand(
                id = "emergency_call",
                phrase = "emergencia",
                action = VoiceAction.START_GROUP_CALL,
                priority = CommandPriority.HIGH,
                metadata = mapOf("type" to "emergency")
            ),
            VoiceCommand(
                id = "send_location",
                phrase = "ubicacion",
                action = VoiceAction.SEND_LOCATION,
                priority = CommandPriority.HIGH
            ),
            VoiceCommand(
                id = "record_audio",
                phrase = "grabar audio",
                action = VoiceAction.START_AUDIO_RECORDING,
                priority = CommandPriority.NORMAL
            ),
            VoiceCommand(
                id = "record_video",
                phrase = "grabar video",
                action = VoiceAction.START_VIDEO_RECORDING,
                priority = CommandPriority.NORMAL
            ),
            VoiceCommand(
                id = "video_call",
                phrase = "llamada",
                action = VoiceAction.START_GROUP_CALL,
                priority = CommandPriority.NORMAL,
                metadata = mapOf("type" to "video")
            ),
            VoiceCommand(
                id = "send_alert",
                phrase = "alerta",
                action = VoiceAction.SEND_MESSAGE,
                priority = CommandPriority.HIGH,
                metadata = mapOf("message" to "⚠️ ALERTA - Requiero asistencia inmediata")
            ),
            VoiceCommand(
                id = "backup_request",
                phrase = "refuerzo",
                action = VoiceAction.SEND_MESSAGE,
                priority = CommandPriority.HIGH,
                metadata = mapOf("message" to "🚨 SOLICITO REFUERZOS - Situación crítica")
            ),
            VoiceCommand(
                id = "status_update",
                phrase = "estado",
                action = VoiceAction.SEND_MESSAGE,
                priority = CommandPriority.NORMAL,
                metadata = mapOf("message" to "📍 Actualizando estado y posición")
            )
        )
    }
    
    /**
     * FUNCIÓN NUEVA: Guarda el ID del chat grupal activo
     */
    suspend fun setCurrentChatId(chatId: String?) {
        prefs.edit()
            .putString(KEY_CURRENT_CHAT_ID, chatId)
            .putLong(KEY_LAST_ACTIVATION_TIME, System.currentTimeMillis())
            .apply()
        
        android.util.Log.d("VoicePreferences", "💾 Chat activo guardado: $chatId")
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene el ID del chat grupal activo
     */
    fun getCurrentChatId(): kotlinx.coroutines.flow.Flow<String?> {
        return kotlinx.coroutines.flow.flow {
            emit(prefs.getString(KEY_CURRENT_CHAT_ID, null))
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Verifica si hay un chat grupal activo
     */
    fun hasActiveGroupChat(): Boolean {
        return !prefs.getString(KEY_CURRENT_CHAT_ID, null).isNullOrEmpty()
    }
    
    /**
     * FUNCIÓN NUEVA: Habilita/deshabilita el modo 24/7
     */
    fun set24x7ModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_24X7_MODE_ENABLED, enabled)
            .apply()
        
        android.util.Log.d("VoicePreferences", "🔄 Modo 24/7 ${if (enabled) "habilitado" else "deshabilitado"}")
    }
    
    /**
     * FUNCIÓN NUEVA: Verifica si el modo 24/7 está habilitado
     */
    fun is24x7ModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_24X7_MODE_ENABLED, true) // Por defecto habilitado
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene el tiempo de la última activación
     */
    fun getLastActivationTime(): Long {
        return prefs.getLong(KEY_LAST_ACTIVATION_TIME, 0L)
    }
    
    /**
     * FUNCIÓN NUEVA: Limpia el estado del chat activo
     */
    fun clearActiveChat() {
        prefs.edit()
            .remove(KEY_CURRENT_CHAT_ID)
            .apply()
        
        android.util.Log.d("VoicePreferences", "🧹 Chat activo limpiado")
    }
    
    /**
     * Obtiene los comandos de voz configurados
     */
    fun getVoiceCommands(): List<VoiceCommand> {
        return try {
            val commandsJson = prefs.getString(KEY_VOICE_COMMANDS, null)
            if (commandsJson != null) {
                val type = object : TypeToken<List<VoiceCommand>>() {}.type
                gson.fromJson(commandsJson, type)
            } else {
                // Primera vez - devolver comandos por defecto
                saveVoiceCommands(DEFAULT_COMMANDS)
                DEFAULT_COMMANDS
            }
        } catch (e: Exception) {
            android.util.Log.e("VoicePreferences", "Error loading voice commands", e)
            DEFAULT_COMMANDS
        }
    }
    
    /**
     * Guarda los comandos de voz
     */
    fun saveVoiceCommands(commands: List<VoiceCommand>) {
        try {
            val commandsJson = gson.toJson(commands)
            prefs.edit()
                .putString(KEY_VOICE_COMMANDS, commandsJson)
                .apply()
                
            android.util.Log.d("VoicePreferences", "✅ Comandos guardados: ${commands.size}")
        } catch (e: Exception) {
            android.util.Log.e("VoicePreferences", "Error saving voice commands", e)
        }
    }
    
    /**
     * Obtiene la sensibilidad configurada
     */
    fun getSensitivity(): Float {
        return prefs.getFloat(KEY_SENSITIVITY, 0.7f)
    }
    
    /**
     * Establece la sensibilidad
     */
    fun setSensitivity(level: Float) {
        prefs.edit()
            .putFloat(KEY_SENSITIVITY, level.coerceIn(0.1f, 1.0f))
            .apply()
    }
    
    /**
     * Verifica si el modo sigiloso está habilitado
     */
    fun isStealthModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_STEALTH_MODE, false)
    }
    
    /**
     * Establece el modo sigiloso
     */
    fun setStealthMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_STEALTH_MODE, enabled)
            .apply()
    }
    
    /**
     * Obtiene el engine preferido
     */
    fun getPreferredEngine(): String {
        return prefs.getString(KEY_PREFERRED_ENGINE, "ANDROID_SPEECH") ?: "ANDROID_SPEECH"
    }
    
    /**
     * Establece el engine preferido
     */
    fun setPreferredEngine(engine: String) {
        prefs.edit()
            .putString(KEY_PREFERRED_ENGINE, engine)
            .apply()
    }
    
    /**
     * Verifica si el fallback automático está habilitado
     */
    fun isAutoFallbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_FALLBACK, true)
    }
    
    /**
     * Establece el fallback automático
     */
    fun setAutoFallback(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_FALLBACK, enabled)
            .apply()
    }
    
    /**
     * Verifica si la optimización de batería está habilitada
     */
    fun isBatteryOptimizationEnabled(): Boolean {
        return prefs.getBoolean(KEY_BATTERY_OPTIMIZATION, true)
    }
    
    /**
     * Establece la optimización de batería
     */
    fun setBatteryOptimization(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BATTERY_OPTIMIZATION, enabled)
            .apply()
    }
    
    /**
     * Obtiene la frase de activación
     */
    fun getWakePhrase(): String {
        return prefs.getString(KEY_WAKE_PHRASE, "asistente") ?: "asistente"
    }
    
    /**
     * Establece la frase de activación
     */
    fun setWakePhrase(phrase: String) {
        prefs.edit()
            .putString(KEY_WAKE_PHRASE, phrase.lowercase().trim())
            .apply()
    }
    
    /**
     * Verifica si la frase de activación está habilitada
     */
    fun isWakePhraseEnabled(): Boolean {
        return prefs.getBoolean(KEY_WAKE_PHRASE_ENABLED, false)
    }
    
    /**
     * Habilita/deshabilita la frase de activación
     */
    fun setWakePhraseEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_WAKE_PHRASE_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Añade un comando personalizado
     */
    fun addCustomCommand(command: VoiceCommand) {
        val currentCommands = getVoiceCommands().toMutableList()
        
        // Remover comando existente con el mismo ID si existe
        currentCommands.removeAll { it.id == command.id }
        
        // Agregar nuevo comando
        currentCommands.add(command)
        
        saveVoiceCommands(currentCommands)
    }
    
    /**
     * Elimina un comando por ID
     */
    fun removeCommand(commandId: String) {
        val currentCommands = getVoiceCommands().toMutableList()
        currentCommands.removeAll { it.id == commandId }
        saveVoiceCommands(currentCommands)
    }
    
    /**
     * Habilita/deshabilita un comando específico
     */
    fun toggleCommandEnabled(commandId: String, enabled: Boolean) {
        val currentCommands = getVoiceCommands().toMutableList()
        val commandIndex = currentCommands.indexOfFirst { it.id == commandId }
        
        if (commandIndex != -1) {
            currentCommands[commandIndex] = currentCommands[commandIndex].copy(isEnabled = enabled)
            saveVoiceCommands(currentCommands)
        }
    }
    
    /**
     * Resetea a configuración por defecto
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        saveVoiceCommands(DEFAULT_COMMANDS)
        android.util.Log.d("VoicePreferences", "🔄 Configuración reseteada a valores por defecto")
    }
    
    /**
     * Exporta configuración como JSON
     */
    fun exportConfiguration(): String {
        val config = mapOf(
            "commands" to getVoiceCommands(),
            "sensitivity" to getSensitivity(),
            "stealthMode" to isStealthModeEnabled(),
            "preferredEngine" to getPreferredEngine(),
            "autoFallback" to isAutoFallbackEnabled(),
            "batteryOptimization" to isBatteryOptimizationEnabled(),
            "wakePhrase" to getWakePhrase(),
            "wakePhraseEnabled" to isWakePhraseEnabled()
        )
        
        return gson.toJson(config)
    }

    /**
     * FUNCIÓN NUEVA: Obtiene la duración de grabación de audio (en segundos)
     */
    fun getAudioRecordingDuration(): Int {
        return prefs.getInt(KEY_AUDIO_RECORDING_DURATION, 30) // 30 segundos por defecto
    }
    
    /**
     * FUNCIÓN NUEVA: Establece la duración de grabación de audio
     */
    fun setAudioRecordingDuration(seconds: Int) {
        prefs.edit()
            .putInt(KEY_AUDIO_RECORDING_DURATION, seconds.coerceIn(5, 300)) // Entre 5 y 300 segundos
            .apply()
        
        android.util.Log.d("VoicePreferences", "🎤 Duración de audio configurada: ${seconds}s")
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene la duración de grabación de video (en segundos)
     */
    fun getVideoRecordingDuration(): Int {
        return prefs.getInt(KEY_VIDEO_RECORDING_DURATION, 15) // 15 segundos por defecto
    }
    
    /**
     * FUNCIÓN NUEVA: Establece la duración de grabación de video
     */
    fun setVideoRecordingDuration(seconds: Int) {
        prefs.edit()
            .putInt(KEY_VIDEO_RECORDING_DURATION, seconds.coerceIn(5, 60)) // Entre 5 y 60 segundos
            .apply()
        
        android.util.Log.d("VoicePreferences", "📹 Duración de video configurada: ${seconds}s")
    }
    
    /**
     * FUNCIÓN NUEVA: Verifica si la captura de fotos está habilitada
     */
    fun isPhotoCaptureEnabled(): Boolean {
        return prefs.getBoolean(KEY_PHOTO_CAPTURE_ENABLED, true)
    }
    
    /**
     * FUNCIÓN NUEVA: Habilita/deshabilita la captura de fotos
     */
    fun setPhotoCaptureEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_PHOTO_CAPTURE_ENABLED, enabled)
            .apply()
        
        android.util.Log.d("VoicePreferences", "📸 Captura de fotos ${if (enabled) "habilitada" else "deshabilitada"}")
    }
    
    /**
     * FUNCIÓN NUEVA: Verifica si el envío automático está habilitado
     */
    fun isAutoSendRecordingsEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SEND_RECORDINGS, true)
    }
    
    /**
     * FUNCIÓN NUEVA: Habilita/deshabilita el envío automático
     */
    fun setAutoSendRecordingsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_SEND_RECORDINGS, enabled)
            .apply()
        
        android.util.Log.d("VoicePreferences", "📤 Envío automático ${if (enabled) "habilitado" else "deshabilitado"}")
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene la calidad de grabación
     */
    fun getRecordingQuality(): String {
        return prefs.getString(KEY_RECORDING_QUALITY, "HIGH") ?: "HIGH"
    }
    
    /**
     * FUNCIÓN NUEVA: Establece la calidad de grabación
     */
    fun setRecordingQuality(quality: String) {
        prefs.edit()
            .putString(KEY_RECORDING_QUALITY, quality)
            .apply()
        
        android.util.Log.d("VoicePreferences", "🎯 Calidad de grabación configurada: $quality")
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene las acciones configuradas para cada comando
     */
    fun getCommandActions(): Map<String, String> {
        val actionsJson = prefs.getString(KEY_COMMAND_ACTIONS, null)
        return if (actionsJson != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(actionsJson, type)
            } catch (e: Exception) {
                getDefaultCommandActions()
            }
        } else {
            getDefaultCommandActions()
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Establece las acciones para cada comando
     */
    fun setCommandActions(actions: Map<String, String>) {
        try {
            val actionsJson = gson.toJson(actions)
            prefs.edit()
                .putString(KEY_COMMAND_ACTIONS, actionsJson)
                .apply()
            
            android.util.Log.d("VoicePreferences", "⚙️ Acciones de comandos configuradas: ${actions.size} comandos")
        } catch (e: Exception) {
            android.util.Log.e("VoicePreferences", "Error guardando acciones de comandos", e)
        }
    }
    
    /**
     * FUNCIÓN NUEVA: Obtiene las acciones por defecto
     */
    private fun getDefaultCommandActions(): Map<String, String> {
        return mapOf(
            "grabar audio" to "AUDIO",
            "grabar video" to "VIDEO", 
            "foto" to "PHOTO",
            "cámara" to "PHOTO",
            "emergencia" to "AUDIO",
            "alerta" to "AUDIO",
            "refuerzo" to "AUDIO",
            "ubicacion" to "LOCATION",
            "estado" to "TEXT"
        )
    }
}