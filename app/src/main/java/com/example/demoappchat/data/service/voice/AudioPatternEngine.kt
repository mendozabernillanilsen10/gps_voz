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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Engine de reconocimiento por patrones de audio
 * Detecta comandos basándose en características acústicas y patrones de frecuencia
 * Útil como fallback cuando otros engines fallan
 */
@Singleton
class AudioPatternEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceEngine {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var callback: ((String, Float) -> Unit)? = null
    private var commands: List<VoiceCommand> = emptyList()
    private var sensitivity: Float = 0.7f
    private var stealthMode: Boolean = false
    
    // Configuración de audio
    private val sampleRate = 16000
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
    
    // Patrones de comandos preconfigurados
    private val commandPatterns = mapOf(
        "emergencia" to AudioPattern(
            minFreq = 200f, maxFreq = 1000f, 
            minDuration = 800, maxDuration = 2000,
            energyThreshold = 0.6f
        ),
        "llamada" to AudioPattern(
            minFreq = 150f, maxFreq = 800f,
            minDuration = 500, maxDuration = 1500,
            energyThreshold = 0.5f
        ),
        "ubicacion" to AudioPattern(
            minFreq = 180f, maxFreq = 900f,
            minDuration = 700, maxDuration = 1800,
            energyThreshold = 0.55f
        ),
        "grabar" to AudioPattern(
            minFreq = 160f, maxFreq = 700f,
            minDuration = 400, maxDuration = 1200,
            energyThreshold = 0.45f
        )
    )
    
    private var currentAudioData = mutableListOf<Short>()
    private var voiceStartTime = 0L
    private var silenceCounter = 0
    private val maxSilenceFrames = 20 // ~400ms de silencio
    
    override suspend fun startListening(): Result<Unit> {
        return try {
            android.util.Log.d("AudioPatternEngine", "🎤 Iniciando Audio Pattern Engine...")
            
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
            
            // Iniciar procesamiento
            scope.launch {
                processAudioPatterns()
            }
            
            android.util.Log.d("AudioPatternEngine", "✅ Audio Pattern Engine iniciado")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AudioPatternEngine", "❌ Error iniciando Audio Pattern Engine", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopListening(): Result<Unit> {
        return try {
            android.util.Log.d("AudioPatternEngine", "🛑 Deteniendo Audio Pattern Engine...")
            
            isListening = false
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            currentAudioData.clear()
            
            android.util.Log.d("AudioPatternEngine", "✅ Audio Pattern Engine detenido")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AudioPatternEngine", "❌ Error deteniendo Audio Pattern Engine", e)
            Result.failure(e)
        }
    }
    
    override fun setCallback(callback: (text: String, confidence: Float) -> Unit) {
        this.callback = callback
    }
    
    override fun updateCommands(commands: List<VoiceCommand>) {
        this.commands = commands
        android.util.Log.d("AudioPatternEngine", "📋 Comandos de patrón actualizados: ${commands.size}")
    }
    
    override fun setSensitivity(level: Float) {
        this.sensitivity = level
        android.util.Log.d("AudioPatternEngine", "🎛️ Sensibilidad de patrón: $level")
    }
    
    override fun setStealthMode(enabled: Boolean) {
        this.stealthMode = enabled
        android.util.Log.d("AudioPatternEngine", "🥷 Modo sigiloso de patrón: $enabled")
    }
    
    private suspend fun processAudioPatterns() {
        try {
            val buffer = ShortArray(bufferSize / 2)
            
            while (isListening && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (samplesRead > 0) {
                    analyzeAudioFrame(buffer, samplesRead)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AudioPatternEngine", "❌ Error procesando patrones de audio", e)
        }
    }
    
    private fun analyzeAudioFrame(buffer: ShortArray, samplesRead: Int) {
        // Calcular energía del frame
        val energy = calculateFrameEnergy(buffer, samplesRead)
        val threshold = 500f // Umbral de detección de voz
        
        if (energy > threshold) {
            // Detectada voz
            if (voiceStartTime == 0L) {
                voiceStartTime = System.currentTimeMillis()
                currentAudioData.clear()
            }
            
            // Agregar datos al buffer
            for (i in 0 until samplesRead) {
                currentAudioData.add(buffer[i])
            }
            
            silenceCounter = 0
            
        } else {
            // Silencio detectado
            silenceCounter++
            
            // Si hay datos acumulados y suficiente silencio, procesar
            if (voiceStartTime > 0 && silenceCounter >= maxSilenceFrames) {
                processVoiceSegment()
                resetVoiceDetection()
            }
        }
        
        // Timeout para evitar buffers muy largos
        if (voiceStartTime > 0 && System.currentTimeMillis() - voiceStartTime > 5000) {
            processVoiceSegment()
            resetVoiceDetection()
        }
    }
    
    private fun calculateFrameEnergy(buffer: ShortArray, samplesRead: Int): Float {
        var sum = 0.0
        for (i in 0 until samplesRead) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / samplesRead).toFloat()
    }
    
    private fun processVoiceSegment() {
        if (currentAudioData.size < 1000) return // Muy corto
        
        try {
            val duration = System.currentTimeMillis() - voiceStartTime
            val features = extractAudioFeatures(currentAudioData.toShortArray())
            
            // Buscar coincidencias con patrones conocidos
            val bestMatch = findBestPatternMatch(features, duration)
            
            if (bestMatch != null) {
                val confidence = calculatePatternConfidence(features, bestMatch.second, duration)
                
                if (confidence >= sensitivity) {
                    android.util.Log.d("AudioPatternEngine", 
                        "🎯 Patrón detectado: '${bestMatch.first}' (${confidence}%)"
                    )
                    callback?.invoke(bestMatch.first, confidence)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AudioPatternEngine", "❌ Error procesando segmento de voz", e)
        }
    }
    
    private fun extractAudioFeatures(audioData: ShortArray): AudioFeatures {
        // Análisis simple de características
        val energy = audioData.map { it * it }.average().toFloat()
        val zeroCrossings = countZeroCrossings(audioData)
        val dominantFreq = estimateDominantFrequency(audioData)
        val spectralCentroid = calculateSpectralCentroid(audioData)
        
        return AudioFeatures(
            energy = energy,
            zeroCrossings = zeroCrossings,
            dominantFrequency = dominantFreq,
            spectralCentroid = spectralCentroid
        )
    }
    
    private fun countZeroCrossings(audioData: ShortArray): Int {
        var crossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i] >= 0) != (audioData[i-1] >= 0)) {
                crossings++
            }
        }
        return crossings
    }
    
    private fun estimateDominantFrequency(audioData: ShortArray): Float {
        // Estimación simple usando zero-crossings
        val zeroCrossings = countZeroCrossings(audioData)
        val duration = audioData.size.toFloat() / sampleRate
        return (zeroCrossings / (2 * duration))
    }
    
    private fun calculateSpectralCentroid(audioData: ShortArray): Float {
        // Cálculo simplificado del centroide espectral
        var weightedSum = 0.0
        var magnitudeSum = 0.0
        
        for (i in audioData.indices) {
            val magnitude = abs(audioData[i].toDouble())
            weightedSum += i * magnitude
            magnitudeSum += magnitude
        }
        
        return if (magnitudeSum > 0) (weightedSum / magnitudeSum).toFloat() else 0f
    }
    
    private fun findBestPatternMatch(features: AudioFeatures, duration: Long): Pair<String, AudioPattern>? {
        var bestMatch: Pair<String, AudioPattern>? = null
        var bestScore = 0f
        
        for ((command, pattern) in commandPatterns) {
            // Verificar si hay comandos habilitados para este patrón
            val hasEnabledCommand = commands.any { cmd ->
                cmd.isEnabled && cmd.phrase.lowercase().contains(command.lowercase())
            }
            
            if (!hasEnabledCommand) continue
            
            val score = calculatePatternScore(features, pattern, duration)
            if (score > bestScore) {
                bestScore = score
                bestMatch = command to pattern
            }
        }
        
        return if (bestScore > 0.3f) bestMatch else null
    }
    
    private fun calculatePatternScore(features: AudioFeatures, pattern: AudioPattern, duration: Long): Float {
        var score = 0f
        
        // Score por frecuencia
        if (features.dominantFrequency in pattern.minFreq..pattern.maxFreq) {
            score += 0.3f
        }
        
        // Score por duración
        if (duration in pattern.minDuration..pattern.maxDuration) {
            score += 0.3f
        }
        
        // Score por energía
        val normalizedEnergy = minOf(1f, features.energy / 10000f)
        if (normalizedEnergy >= pattern.energyThreshold) {
            score += 0.4f
        }
        
        return score
    }
    
    private fun calculatePatternConfidence(features: AudioFeatures, pattern: AudioPattern, duration: Long): Float {
        val score = calculatePatternScore(features, pattern, duration)
        return minOf(1f, score + 0.2f) // Boost base
    }
    
    private fun resetVoiceDetection() {
        voiceStartTime = 0L
        currentAudioData.clear()
        silenceCounter = 0
    }
}

/**
 * Características extraídas del audio
 */
data class AudioFeatures(
    val energy: Float,
    val zeroCrossings: Int,
    val dominantFrequency: Float,
    val spectralCentroid: Float
)

/**
 * Patrón de reconocimiento para un comando específico
 */
data class AudioPattern(
    val minFreq: Float,
    val maxFreq: Float,
    val minDuration: Long,
    val maxDuration: Long,
    val energyThreshold: Float
)