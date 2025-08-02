package com.example.demoappchat.data.service.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.demoappchat.domain.model.VoiceCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación profesional del engine Vosk para reconocimiento offline
 * Optimizado para comandos de voz en español con baja latencia
 */
@Singleton
class VoskEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceEngine {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var callback: ((String, Float) -> Unit)? = null
    private var commands: List<VoiceCommand> = emptyList()
    private var sensitivity: Float = 0.7f
    private var stealthMode: Boolean = false
    
    // Configuración de audio optimizada
    private val sampleRate = 16000
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
    
    override suspend fun startListening(): Result<Unit> {
        return try {
            android.util.Log.d("VoskEngine", "🎤 Iniciando Vosk Engine...")
            
            // Inicializar modelo si no existe
            if (model == null) {
                initializeVoskModel()
            }
            
            // Crear recognizer
            recognizer = Recognizer(model, sampleRate.toFloat())
            
            // Configurar AudioRecord
            audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return Result.failure(Exception("AudioRecord no se pudo inicializar"))
            }
            
            // Iniciar grabación
            audioRecord?.startRecording()
            isListening = true
            
            // Iniciar procesamiento de audio en background
            scope.launch {
                processAudioStream()
            }
            
            android.util.Log.d("VoskEngine", "✅ Vosk Engine iniciado exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error iniciando Vosk Engine", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopListening(): Result<Unit> {
        return try {
            android.util.Log.d("VoskEngine", "🛑 Deteniendo Vosk Engine...")
            
            isListening = false
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            recognizer?.close()
            recognizer = null
            
            android.util.Log.d("VoskEngine", "✅ Vosk Engine detenido")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error deteniendo Vosk Engine", e)
            Result.failure(e)
        }
    }
    
    override fun setCallback(callback: (text: String, confidence: Float) -> Unit) {
        this.callback = callback
    }
    
    override fun updateCommands(commands: List<VoiceCommand>) {
        this.commands = commands
        android.util.Log.d("VoskEngine", "📋 Comandos Vosk actualizados: ${commands.size}")
    }
    
    override fun setSensitivity(level: Float) {
        this.sensitivity = level
        android.util.Log.d("VoskEngine", "🎛️ Sensibilidad Vosk: $level")
    }
    
    override fun setStealthMode(enabled: Boolean) {
        this.stealthMode = enabled
        android.util.Log.d("VoskEngine", "🥷 Modo sigiloso Vosk: $enabled")
    }
    
    private fun initializeVoskModel() {
        try {
            LibVosk.setLogLevel(if (stealthMode) LogLevel.WARNINGS else LogLevel.INFO)
            
            // Ruta del modelo Vosk
            val modelPath = File(context.filesDir, "vosk-model")
            
            if (!modelPath.exists()) {
                throw Exception("Modelo Vosk no encontrado en ${modelPath.absolutePath}")
            }
            
            model = Model(modelPath.absolutePath)
            android.util.Log.d("VoskEngine", "✅ Modelo Vosk cargado desde ${modelPath.absolutePath}")
            
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error cargando modelo Vosk", e)
            throw e
        }
    }
    
    private suspend fun processAudioStream() {
        try {
            val buffer = ShortArray(bufferSize / 2)
            
            while (isListening && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // Convertir a bytes para Vosk
                    val audioData = ByteArray(bytesRead * 2)
                    for (i in 0 until bytesRead) {
                        val sample = buffer[i]
                        audioData[i * 2] = (sample.toInt() and 0xff).toByte()
                        audioData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
                    }
                    
                    // Procesar con Vosk
                    processVoskAudio(audioData)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error procesando stream de audio", e)
        }
    }
    
    private fun processVoskAudio(audioData: ByteArray) {
        try {
            recognizer?.let { rec ->
                if (rec.acceptWaveForm(audioData, audioData.size)) {
                    // Resultado final
                    val result = rec.result
                    parseVoskResult(result, false)
                } else {
                    // Resultado parcial (solo si no estamos en modo sigiloso)
                    if (!stealthMode) {
                        val partialResult = rec.partialResult
                        parseVoskResult(partialResult, true)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error procesando audio Vosk", e)
        }
    }
    
    private fun parseVoskResult(jsonResult: String, isPartial: Boolean) {
        try {
            // Parse simple del JSON de Vosk
            val text = extractTextFromVoskJson(jsonResult)
            
            if (text.isNotBlank()) {
                // Calcular confianza basada en longitud y coincidencias
                val confidence = calculateConfidence(text)
                
                android.util.Log.d("VoskEngine", 
                    if (isPartial) "🔄 Vosk Parcial: '$text' (${confidence}%)"
                    else "🎯 Vosk Final: '$text' (${confidence}%)"
                )
                
                // Filtrar por sensibilidad y verificar comandos
                if (confidence >= sensitivity && matchesCommand(text)) {
                    android.util.Log.d("VoskEngine", "✅ Comando Vosk detectado: '$text'")
                    callback?.invoke(text, confidence)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("VoskEngine", "❌ Error parseando resultado Vosk", e)
        }
    }
    
    private fun extractTextFromVoskJson(json: String): String {
        return try {
            // Parse simple para extraer texto del JSON de Vosk
            val textKey = "\"text\" : \""
            val startIndex = json.indexOf(textKey)
            if (startIndex == -1) return ""
            
            val textStart = startIndex + textKey.length
            val textEnd = json.indexOf("\"", textStart)
            if (textEnd == -1) return ""
            
            json.substring(textStart, textEnd).trim()
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun calculateConfidence(text: String): Float {
        // Algoritmo simple para calcular confianza
        var confidence = 0.5f
        
        // Bonus por longitud adecuada
        if (text.length in 3..20) confidence += 0.2f
        
        // Bonus por coincidencia con comandos conocidos
        if (matchesCommand(text)) confidence += 0.3f
        
        // Bonus por palabras completas
        if (text.contains(" ")) confidence += 0.1f
        
        return minOf(1.0f, confidence)
    }
    
    private fun matchesCommand(text: String): Boolean {
        if (commands.isEmpty()) return false
        
        val lowercaseText = text.lowercase()
        
        return commands.any { command ->
            command.isEnabled && lowercaseText.contains(command.phrase.lowercase())
        }
    }
}