package com.example.demoappchat.data.service.voice

import android.content.Context
import com.example.demoappchat.domain.model.VoiceCommand
import com.example.demoappchat.domain.model.VoiceRecognitionResult
import com.example.demoappchat.domain.model.RecognitionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager profesional para múltiples engines de reconocimiento de voz
 * Implementa estrategia de fallback automático y optimización de batería
 */
@Singleton
class VoiceEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val androidSpeechEngine: AndroidSpeechEngine,
    private val voskEngine: VoskEngine,
    private val audioPatternEngine: AudioPatternEngine
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _currentEngine = MutableStateFlow<VoiceEngine?>(null)
    private val _isListening = MutableStateFlow(false)
    private val _engineStatus = MutableStateFlow<EngineStatus>(EngineStatus.Stopped)
    
    private var recognitionCallback: ((VoiceRecognitionResult) -> Unit)? = null
    private var currentCommands: List<VoiceCommand> = emptyList()
    private var sensitivity: Float = 0.7f
    private var stealthMode: Boolean = false
    
    // Prioridad de engines (del mejor al peor)
    private val enginePriority = listOf(
        RecognitionSource.ANDROID_SPEECH,
        RecognitionSource.VOSK,
        RecognitionSource.AUDIO_PATTERN
    )
    
    suspend fun startRecognition(): Result<Unit> {
        return try {
            android.util.Log.d("VoiceEngineManager", "🚀 Iniciando reconocimiento con estrategia de fallback")
            
            _engineStatus.value = EngineStatus.Starting
            
            // Intentar iniciar engines en orden de prioridad
            for (engineType in enginePriority) {
                val engine = getEngineByType(engineType)
                val result = tryStartEngine(engine, engineType)
                
                if (result.isSuccess) {
                    _currentEngine.value = engine
                    _isListening.value = true
                    _engineStatus.value = EngineStatus.Active(engineType)
                    
                    android.util.Log.d("VoiceEngineManager", "✅ Engine activo: $engineType")
                    return Result.success(Unit)
                } else {
                    android.util.Log.w("VoiceEngineManager", "⚠️ Engine $engineType falló: ${result.exceptionOrNull()?.message}")
                }
            }
            
            // Si todos fallan
            _engineStatus.value = EngineStatus.Failed("Todos los engines fallaron")
            Result.failure(Exception("No se pudo iniciar ningún engine de reconocimiento"))
            
        } catch (e: Exception) {
            android.util.Log.e("VoiceEngineManager", "❌ Error crítico iniciando reconocimiento", e)
            _engineStatus.value = EngineStatus.Failed(e.message ?: "Error desconocido")
            Result.failure(e)
        }
    }
    
    suspend fun stopRecognition(): Result<Unit> {
        return try {
            android.util.Log.d("VoiceEngineManager", "🛑 Deteniendo reconocimiento...")
            
            _currentEngine.value?.stopListening()
            _currentEngine.value = null
            _isListening.value = false
            _engineStatus.value = EngineStatus.Stopped
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("VoiceEngineManager", "❌ Error deteniendo reconocimiento", e)
            Result.failure(e)
        }
    }
    
    fun setRecognitionCallback(callback: (VoiceRecognitionResult) -> Unit) {
        this.recognitionCallback = callback
    }
    
    fun updateCommands(commands: List<VoiceCommand>) {
        this.currentCommands = commands
        _currentEngine.value?.updateCommands(commands)
    }
    
    fun updateSensitivity(level: Float) {
        this.sensitivity = level
        _currentEngine.value?.setSensitivity(level)
    }
    
    fun setStealthMode(enabled: Boolean) {
        this.stealthMode = enabled
        _currentEngine.value?.setStealthMode(enabled)
    }
    
    fun isListening(): Boolean = _isListening.value
    
    fun getCurrentEngine(): String {
        return when (val status = _engineStatus.value) {
            is EngineStatus.Active -> status.type.name
            is EngineStatus.Starting -> "starting"
            is EngineStatus.Stopped -> "stopped"
            is EngineStatus.Failed -> "failed"
        }
    }
    
    fun getEngineStatus(): StateFlow<EngineStatus> = _engineStatus
    
    private suspend fun tryStartEngine(engine: VoiceEngine, type: RecognitionSource): Result<Unit> {
        return try {
            // Configurar callback del engine
            engine.setCallback { text, confidence ->
                scope.launch {
                    handleRecognitionResult(text, confidence, type)
                }
            }
            
            // Configurar parámetros
            engine.updateCommands(currentCommands)
            engine.setSensitivity(sensitivity)
            engine.setStealthMode(stealthMode)
            
            // Iniciar reconocimiento
            engine.startListening()
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun handleRecognitionResult(text: String, confidence: Float, source: RecognitionSource) {
        val result = VoiceRecognitionResult(
            text = text,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            source = source
        )
        
        android.util.Log.d("VoiceEngineManager", "🎯 Resultado: '$text' (${confidence}%) desde $source")
        
        // Si la confianza es muy baja, intentar con el siguiente engine
        if (confidence < 0.3f && canFallbackToNextEngine()) {
            android.util.Log.d("VoiceEngineManager", "🔄 Confianza baja, intentando engine de respaldo...")
            scope.launch {
                fallbackToNextEngine()
            }
            return
        }
        
        // Enviar resultado al callback
        recognitionCallback?.invoke(result)
    }
    
    private fun canFallbackToNextEngine(): Boolean {
        val currentType = (_engineStatus.value as? EngineStatus.Active)?.type
        val currentIndex = enginePriority.indexOf(currentType)
        return currentIndex < enginePriority.size - 1
    }
    
    private suspend fun fallbackToNextEngine() {
        val currentType = (_engineStatus.value as? EngineStatus.Active)?.type
        val currentIndex = enginePriority.indexOf(currentType)
        
        if (currentIndex < enginePriority.size - 1) {
            // Detener engine actual
            _currentEngine.value?.stopListening()
            
            // Intentar siguiente engine
            val nextType = enginePriority[currentIndex + 1]
            val nextEngine = getEngineByType(nextType)
            val result = tryStartEngine(nextEngine, nextType)
            
            if (result.isSuccess) {
                _currentEngine.value = nextEngine
                _engineStatus.value = EngineStatus.Active(nextType)
                android.util.Log.d("VoiceEngineManager", "✅ Fallback a $nextType exitoso")
            }
        }
    }
    
    private fun getEngineByType(type: RecognitionSource): VoiceEngine {
        return when (type) {
            RecognitionSource.ANDROID_SPEECH -> androidSpeechEngine
            RecognitionSource.VOSK -> voskEngine
            RecognitionSource.AUDIO_PATTERN -> audioPatternEngine
        }
    }
}

/**
 * Interface unificada para engines de reconocimiento
 */
interface VoiceEngine {
    suspend fun startListening(): Result<Unit>
    suspend fun stopListening(): Result<Unit>
    fun setCallback(callback: (text: String, confidence: Float) -> Unit)
    fun updateCommands(commands: List<VoiceCommand>)
    fun setSensitivity(level: Float)
    fun setStealthMode(enabled: Boolean)
}

/**
 * Estados del engine manager
 */
sealed class EngineStatus {
    object Stopped : EngineStatus()
    object Starting : EngineStatus()
    data class Active(val type: RecognitionSource) : EngineStatus()
    data class Failed(val reason: String) : EngineStatus()
}