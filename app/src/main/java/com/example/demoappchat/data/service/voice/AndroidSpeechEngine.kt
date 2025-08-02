package com.example.demoappchat.data.service.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.demoappchat.domain.model.VoiceCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación profesional del engine de Android Speech Recognition
 * Maneja reconocimiento continuo con reinicio automático y optimización de batería
 */
@Singleton
class AndroidSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceEngine {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var callback: ((String, Float) -> Unit)? = null
    private var commands: List<VoiceCommand> = emptyList()
    private var sensitivity: Float = 0.7f
    private var stealthMode: Boolean = false
    
    // Configuración del reconocimiento
    private var restartAttempts = 0
    private val maxRestartAttempts = 5
    private val restartDelayMs = 1000L
    
    override suspend fun startListening(): Result<Unit> {
        return try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                return Result.failure(Exception("SpeechRecognizer no disponible"))
            }
            
            android.util.Log.d("AndroidSpeechEngine", "🎤 Iniciando Android Speech Recognition...")
            
            // Asegurar que se ejecute en Main Thread
            scope.launch {
                initializeSpeechRecognizer()
                startRecognitionInternal()
                isListening = true
                restartAttempts = 0
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Error iniciando recognition", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopListening(): Result<Unit> {
        return try {
            android.util.Log.d("AndroidSpeechEngine", "🛑 Deteniendo Android Speech Recognition...")
            
            scope.launch {
                isListening = false
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Error deteniendo recognition", e)
            Result.failure(e)
        }
    }
    
    override fun setCallback(callback: (text: String, confidence: Float) -> Unit) {
        this.callback = callback
    }
    
    override fun updateCommands(commands: List<VoiceCommand>) {
        this.commands = commands
        android.util.Log.d("AndroidSpeechEngine", "📋 Comandos actualizados: ${commands.size}")
    }
    
    override fun setSensitivity(level: Float) {
        this.sensitivity = level
        android.util.Log.d("AndroidSpeechEngine", "🎛️ Sensibilidad: $level")
    }
    
    override fun setStealthMode(enabled: Boolean) {
        this.stealthMode = enabled
        android.util.Log.d("AndroidSpeechEngine", "🥷 Modo sigiloso: $enabled")
    }
    
    private fun initializeSpeechRecognizer() {
        try {
            // Limpiar instancia anterior
            speechRecognizer?.destroy()
            
            // Crear nueva instancia
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            
            android.util.Log.d("AndroidSpeechEngine", "✅ SpeechRecognizer inicializado")
            
        } catch (e: Exception) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Error inicializando SpeechRecognizer", e)
            throw e
        }
    }
    
    private fun startRecognitionInternal() {
        try {
            val intent = createRecognitionIntent()
            speechRecognizer?.startListening(intent)
            android.util.Log.d("AndroidSpeechEngine", "🔊 Reconocimiento iniciado")
            
        } catch (e: Exception) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Error iniciando reconocimiento interno", e)
            scheduleRestart()
        }
    }
    
    private fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            
            // Configuración optimizada para comandos cortos
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
            
            // Configuración de confidencia
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
        }
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            
            override fun onReadyForSpeech(params: Bundle?) {
                android.util.Log.d("AndroidSpeechEngine", "🎯 Listo para reconocimiento")
            }
            
            override fun onBeginningOfSpeech() {
                android.util.Log.d("AndroidSpeechEngine", "🔊 Detectando voz...")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Nivel de audio - opcional para debug
                if (!stealthMode && rmsdB > 5) {
                    android.util.Log.v("AndroidSpeechEngine", "🔉 Nivel audio: $rmsdB dB")
                }
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer recibido - no usado actualmente
            }
            
            override fun onEndOfSpeech() {
                android.util.Log.d("AndroidSpeechEngine", "🔇 Fin de detección de voz")
            }
            
            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                android.util.Log.w("AndroidSpeechEngine", "⚠️ Error: $errorMessage")
                
                // No reiniciar en errores de cliente o permisos
                if (error != SpeechRecognizer.ERROR_CLIENT && 
                    error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    scheduleRestart()
                }
            }
            
            override fun onResults(results: Bundle?) {
                handleRecognitionResults(results, false)
                scheduleRestart() // Reiniciar para reconocimiento continuo
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                if (!stealthMode) { // Solo procesar parciales en modo no sigiloso
                    handleRecognitionResults(partialResults, true)
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                android.util.Log.d("AndroidSpeechEngine", "📡 Evento: $eventType")
            }
        }
    }
    
    private fun handleRecognitionResults(results: Bundle?, isPartial: Boolean) {
        try {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            
            if (matches.isNullOrEmpty()) return
            
            val recognizedText = matches[0]
            val confidence = confidences?.getOrNull(0) ?: 0.5f
            
            android.util.Log.d("AndroidSpeechEngine", 
                if (isPartial) "🔄 Parcial: '$recognizedText' (${confidence}%)"
                else "🎯 Final: '$recognizedText' (${confidence}%)"
            )
            
            // Filtrar por sensibilidad
            if (confidence >= sensitivity) {
                // Verificar si coincide con algún comando
                if (matchesCommand(recognizedText)) {
                    android.util.Log.d("AndroidSpeechEngine", "✅ Comando detectado: '$recognizedText'")
                    callback?.invoke(recognizedText, confidence)
                } else if (!isPartial) {
                    android.util.Log.d("AndroidSpeechEngine", "🔍 No coincide con comandos configurados")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Error procesando resultados", e)
        }
    }
    
    private fun matchesCommand(text: String): Boolean {
        if (commands.isEmpty()) return false
        
        val lowercaseText = text.lowercase()
        
        return commands.any { command ->
            command.isEnabled && lowercaseText.contains(command.phrase.lowercase())
        }
    }
    
    private fun scheduleRestart() {
        if (!isListening) return
        
        if (restartAttempts >= maxRestartAttempts) {
            android.util.Log.e("AndroidSpeechEngine", "❌ Máximo de reintentos alcanzado")
            isListening = false
            return
        }
        
        restartAttempts++
        android.util.Log.d("AndroidSpeechEngine", "🔄 Programando reinicio #$restartAttempts...")
        
        scope.launch {
            delay(restartDelayMs * restartAttempts) // Delay incremental
            
            if (isListening) {
                try {
                    speechRecognizer?.cancel()
                    delay(500) // Pausa breve
                    startRecognitionInternal()
                } catch (e: Exception) {
                    android.util.Log.e("AndroidSpeechEngine", "❌ Error en reinicio", e)
                }
            }
        }
    }
    
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
            SpeechRecognizer.ERROR_NO_MATCH -> "Sin coincidencias"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de voz"
            else -> "Error desconocido: $error"
        }
    }
}