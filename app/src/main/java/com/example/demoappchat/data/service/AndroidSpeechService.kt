package com.example.demoappchat.data.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.demoappchat.data.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SISTEMA DE RECONOCIMIENTO DE VOZ POLICIAL NATIVO
 * Más eficiente que Vosk, sin archivos externos
 */
@Singleton
class AndroidSpeechService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var onCommandDetected: ((String) -> Unit)? = null
    
    // Comandos policiales predefinidos - AMPLIADOS PARA MEJOR DETECCIÓN
    private val policeCommands = listOf(
        // Comandos básicos
        "ayuda", "óyeme", "hola", "alerta", "socorro", "auxilio",
        
        // Comandos de emergencia
        "emergencia", "código rojo", "peligro", "mayday",
        
        // Comandos de grabación
        "grabar", "grabar audio", "grabar video", "evidencia", "testigo",
        
        // Comandos de situación
        "asalto", "robo", "secuestro", "sospechoso", "delito",
        
        // Comandos de respaldo
        "backup", "refuerzo", "unidad", "patrulla", "apoyo",
        
        // Comandos simples para pruebas
        "test", "prueba", "activar", "comando", "policía"
    )
    
    fun startListening(onCommand: (String) -> Unit) {
        this.onCommandDetected = onCommand
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("AndroidSpeech", "❌ Reconocimiento de voz no disponible")
            return
        }
        
        try {
            // CRÍTICO: SpeechRecognizer debe ejecutarse en el Main Thread
            CoroutineScope(Dispatchers.Main).launch {
                stopListening() // Limpiar instancia anterior
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") // Español
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    
                    // Configuración para operaciones policiales
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
                }
                
                speechRecognizer?.startListening(intent)
                isListening = true
                
                Log.d("AndroidSpeech", "🎤 MONITOREO POLICIAL activado - Escuchando comandos...")
            }
            
        } catch (e: Exception) {
            Log.e("AndroidSpeech", "❌ Error iniciando reconocimiento", e)
        }
    }
    
    fun stopListening() {
        try {
            // CRÍTICO: También debe ejecutarse en Main Thread
            CoroutineScope(Dispatchers.Main).launch {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
                
                Log.d("AndroidSpeech", "🔇 Monitoreo de voz detenido")
            }
        } catch (e: Exception) {
            Log.e("AndroidSpeech", "❌ Error deteniendo reconocimiento", e)
        }
    }
    
    private fun createRecognitionListener() = object : RecognitionListener {
        
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("AndroidSpeech", "🎯 Listo para recibir comandos policiales")
        }
        
        override fun onBeginningOfSpeech() {
            Log.d("AndroidSpeech", "🔊 Detectando voz...")
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Monitoreo del nivel de audio (opcional)
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Buffer de audio recibido
        }
        
        override fun onEndOfSpeech() {
            Log.d("AndroidSpeech", "🔇 Fin de detección de voz")
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
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
            
            Log.w("AndroidSpeech", "⚠️ $errorMessage")
            
            // Reiniciar automáticamente para operaciones continuas
            if (isListening) {
                restartListening()
            }
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            
            matches?.firstOrNull()?.let { recognizedText ->
                Log.d("AndroidSpeech", "🎯 Texto reconocido: '$recognizedText'")
                
                // Procesar comando policial
                processPoliceCommand(recognizedText)
            }
            
            // SIEMPRE reiniciar escucha continua para monitoreo 24/7
            if (isListening) {
                restartListening()
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { partialText ->
                Log.d("AndroidSpeech", "🔄 Resultado parcial: '$partialText'")
                
                // Verificar comandos urgentes en tiempo real
                if (isUrgentCommand(partialText)) {
                    processPoliceCommand(partialText)
                }
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d("AndroidSpeech", "📡 Evento: $eventType")
        }
    }
    
    private fun processPoliceCommand(recognizedText: String) {
        val text = recognizedText.lowercase().trim()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar si hay chat activo
                val currentChatId = userPreferences.getCurrentChatId().first()
                if (currentChatId.isNullOrEmpty()) {
                    Log.d("AndroidSpeech", "⚠️ Sin chat activo - comando ignorado")
                    return@launch
                }
                
                // Verificar comandos configurados por el usuario
                val userCommands = userPreferences.getVoiceCommands().first()
                val allCommands = userCommands + policeCommands
                
                Log.d("AndroidSpeech", "🔍 Analizando texto: '$text'")
                Log.d("AndroidSpeech", "📋 Comandos disponibles: ${allCommands.joinToString()}")
                
                // Buscar coincidencias más flexibles
                val matchedCommand = allCommands.find { command ->
                    val commandLower = command.lowercase()
                    // Buscar coincidencia exacta o parcial
                    text.contains(commandLower) || 
                    commandLower.contains(text) ||
                    // Buscar por palabras individuales
                    text.split(" ").any { word -> word == commandLower } ||
                    commandLower.split(" ").any { word -> text.contains(word) }
                }
                
                if (matchedCommand != null) {
                    Log.d("AndroidSpeech", "🚨 COMANDO POLICIAL DETECTADO: '$matchedCommand' en texto '$text'")
                    onCommandDetected?.invoke(recognizedText)
                } else {
                    Log.d("AndroidSpeech", "🔍 Comando no reconocido: '$text'")
                    Log.d("AndroidSpeech", "💡 Palabras detectadas: ${text.split(" ").joinToString()}")
                }
                
            } catch (e: Exception) {
                Log.e("AndroidSpeech", "❌ Error procesando comando", e)
            }
        }
    }
    
    private fun isUrgentCommand(text: String): Boolean {
        val urgentCommands = listOf(
            "emergencia", "auxilio", "socorro", "código rojo", "peligro"
        )
        
        return urgentCommands.any { text.lowercase().contains(it) }
    }
    
    private fun restartListening() {
        try {
            // Pequeña pausa antes de reiniciar
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(100)
                if (isListening) {
                    onCommandDetected?.let { callback ->
                        startListening(callback)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidSpeech", "❌ Error reiniciando reconocimiento", e)
        }
    }
    
    fun isCurrentlyListening(): Boolean = isListening
}