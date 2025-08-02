package com.example.demoappchat.data.repository

import com.example.demoappchat.data.local.preferences.VoicePreferences
import com.example.demoappchat.data.service.voice.VoiceEngineManager
import com.example.demoappchat.domain.model.VoiceCommand
import com.example.demoappchat.domain.model.VoiceRecognitionResult
import com.example.demoappchat.domain.repository.VoiceRepository
import com.example.demoappchat.domain.repository.VoiceServiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación profesional del repositorio de voz
 * Separa concerns y maneja múltiples engines de reconocimiento
 */
@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val voiceEngineManager: VoiceEngineManager,
    private val voicePreferences: VoicePreferences
) : VoiceRepository {
    
    private val _recognitionResults = MutableStateFlow<VoiceRecognitionResult?>(null)
    private val _isActive = MutableStateFlow(false)
    private val _serviceStatus = MutableStateFlow(
        VoiceServiceStatus(
            isActive = false,
            isListening = false,
            currentEngine = "none",
            batteryOptimized = false,
            lastCommand = null,
            errorMessage = null
        )
    )
    
    override suspend fun startVoiceRecognition(): Result<Unit> {
        return try {
            android.util.Log.d("VoiceRepo", "🎤 Iniciando reconocimiento de voz...")
            
            // Configurar callback para resultados
            voiceEngineManager.setRecognitionCallback { result ->
                _recognitionResults.value = result
                updateServiceStatus()
            }
            
            // Iniciar engine
            val result = voiceEngineManager.startRecognition()
            
            if (result.isSuccess) {
                _isActive.value = true
                updateServiceStatus()
                android.util.Log.d("VoiceRepo", "✅ Reconocimiento iniciado exitosamente")
            } else {
                android.util.Log.e("VoiceRepo", "❌ Error iniciando reconocimiento")
                updateServiceStatus(error = result.exceptionOrNull()?.message)
            }
            
            result
            
        } catch (e: Exception) {
            android.util.Log.e("VoiceRepo", "❌ Excepción iniciando reconocimiento", e)
            updateServiceStatus(error = e.message)
            Result.failure(e)
        }
    }
    
    override suspend fun stopVoiceRecognition(): Result<Unit> {
        return try {
            android.util.Log.d("VoiceRepo", "🛑 Deteniendo reconocimiento de voz...")
            
            val result = voiceEngineManager.stopRecognition()
            
            _isActive.value = false
            _recognitionResults.value = null
            updateServiceStatus()
            
            android.util.Log.d("VoiceRepo", "✅ Reconocimiento detenido")
            result
            
        } catch (e: Exception) {
            android.util.Log.e("VoiceRepo", "❌ Error deteniendo reconocimiento", e)
            Result.failure(e)
        }
    }
    
    override fun observeVoiceRecognition(): Flow<VoiceRecognitionResult> {
        return _recognitionResults.filterNotNull()
    }
    
    override suspend fun setVoiceCommands(commands: List<VoiceCommand>): Result<Unit> {
        return try {
            voicePreferences.saveVoiceCommands(commands)
            voiceEngineManager.updateCommands(commands)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVoiceCommands(): Result<List<VoiceCommand>> {
        return try {
            val commands = voicePreferences.getVoiceCommands()
            Result.success(commands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun isVoiceRecognitionActive(): Flow<Boolean> {
        return _isActive
    }
    
    override fun getVoiceServiceStatus(): Flow<VoiceServiceStatus> {
        return _serviceStatus
    }
    
    override suspend fun setSensitivity(level: Float): Result<Unit> {
        return try {
            voicePreferences.setSensitivity(level)
            voiceEngineManager.updateSensitivity(level)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setStealthMode(enabled: Boolean): Result<Unit> {
        return try {
            voicePreferences.setStealthMode(enabled)
            voiceEngineManager.setStealthMode(enabled)
            updateServiceStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun updateServiceStatus(error: String? = null) {
        val currentStatus = _serviceStatus.value
        _serviceStatus.value = currentStatus.copy(
            isActive = _isActive.value,
            isListening = voiceEngineManager.isListening(),
            currentEngine = voiceEngineManager.getCurrentEngine(),
            batteryOptimized = false, // TODO: implementar detección
            lastCommand = _recognitionResults.value,
            errorMessage = error
        )
    }
}

// Extension para filtrar nulls en Flow
private fun <T> Flow<T?>.filterNotNull(): Flow<T> {
    return this.filter { it != null }.map { it!! }
}