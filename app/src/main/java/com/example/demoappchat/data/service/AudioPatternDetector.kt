package com.example.demoappchat.data.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * DETECTOR DE PATRONES DE AUDIO PARA COMANDOS POLICIALES
 * Sistema ultra-liviano que detecta patrones específicos sin reconocimiento de voz completo
 */
@Singleton
class AudioPatternDetector @Inject constructor() {
    
    private var audioRecord: AudioRecord? = null
    private var isDetecting = false
    private var detectionJob: Job? = null
    private var onPatternDetected: ((String) -> Unit)? = null
    
    // Configuración de audio
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // Patrones predefinidos (duración de sonido en ms)
    private val patterns = mapOf(
        "tap_tap_tap" to listOf(200, 100, 200, 100, 200), // 3 golpes rápidos
        "long_short_long" to listOf(800, 200, 400, 200, 800), // SOS en sonido
        "whistle_pattern" to listOf(1000, 300, 500), // Silbido largo-corto
        "clap_sequence" to listOf(150, 150, 150, 150, 150) // 5 palmadas rápidas
    )
    
    fun startDetection(onPattern: (String) -> Unit) {
        this.onPatternDetected = onPattern
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioPattern", "❌ Error inicializando AudioRecord")
                return
            }
            
            audioRecord?.startRecording()
            isDetecting = true
            
            detectionJob = CoroutineScope(Dispatchers.IO).launch {
                detectAudioPatterns(bufferSize)
            }
            
            Log.d("AudioPattern", "🎵 Detector de patrones policiales activado")
            
        } catch (e: Exception) {
            Log.e("AudioPattern", "❌ Error iniciando detección", e)
        }
    }
    
    fun stopDetection() {
        try {
            isDetecting = false
            detectionJob?.cancel()
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            Log.d("AudioPattern", "🔇 Detector de patrones detenido")
        } catch (e: Exception) {
            Log.e("AudioPattern", "❌ Error deteniendo detección", e)
        }
    }
    
    private suspend fun detectAudioPatterns(bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val volumeHistory = mutableListOf<Pair<Long, Double>>()
        val maxHistorySize = 100 // Mantener último segundo de historia
        
        while (isDetecting) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // Calcular volumen RMS
                    val rms = calculateRMS(buffer, bytesRead)
                    val currentTime = System.currentTimeMillis()
                    
                    // Mantener historia de volúmenes
                    volumeHistory.add(currentTime to rms)
                    if (volumeHistory.size > maxHistorySize) {
                        volumeHistory.removeAt(0)
                    }
                    
                    // Detectar patrones en la historia reciente
                    detectPatterns(volumeHistory)
                }
                
                delay(50) // Verificar cada 50ms
                
            } catch (e: Exception) {
                Log.e("AudioPattern", "❌ Error en detección de patrones", e)
                break
            }
        }
    }
    
    private fun calculateRMS(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        return sqrt(sum / length)
    }
    
    private fun detectPatterns(volumeHistory: List<Pair<Long, Double>>) {
        if (volumeHistory.size < 10) return
        
        // Detectar picos de audio (sonidos fuertes)
        val threshold = calculateDynamicThreshold(volumeHistory)
        val peaks = findPeaks(volumeHistory, threshold)
        
        if (peaks.size >= 3) {
            // Analizar intervalos entre picos
            val intervals = calculateIntervals(peaks)
            
            // Comparar con patrones conocidos
            patterns.forEach { (patternName, expectedIntervals) ->
                if (matchesPattern(intervals, expectedIntervals)) {
                    Log.d("AudioPattern", "🚨 PATRÓN DETECTADO: $patternName")
                    onPatternDetected?.invoke(patternName)
                    return // Solo reportar el primer patrón encontrado
                }
            }
        }
    }
    
    private fun calculateDynamicThreshold(volumeHistory: List<Pair<Long, Double>>): Double {
        val volumes = volumeHistory.map { it.second }
        val average = volumes.average()
        val maxVolume = volumes.maxOrNull() ?: 0.0
        
        // Threshold dinámico basado en el ruido ambiente
        return average + (maxVolume - average) * 0.3
    }
    
    private fun findPeaks(volumeHistory: List<Pair<Long, Double>>, threshold: Double): List<Long> {
        val peaks = mutableListOf<Long>()
        
        for (i in 1 until volumeHistory.size - 1) {
            val (time, volume) = volumeHistory[i]
            val prevVolume = volumeHistory[i - 1].second
            val nextVolume = volumeHistory[i + 1].second
            
            // Es un pico si es mayor que los vecinos y supera el threshold
            if (volume > prevVolume && volume > nextVolume && volume > threshold) {
                // Evitar picos muy cercanos (debounce)
                if (peaks.isEmpty() || time - peaks.last() > 80) {
                    peaks.add(time)
                }
            }
        }
        
        return peaks
    }
    
    private fun calculateIntervals(peaks: List<Long>): List<Long> {
        val intervals = mutableListOf<Long>()
        
        for (i in 1 until peaks.size) {
            intervals.add(peaks[i] - peaks[i - 1])
        }
        
        return intervals
    }
    
    private fun matchesPattern(intervals: List<Long>, expectedPattern: List<Int>): Boolean {
        if (intervals.size < expectedPattern.size - 1) return false
        
        // Tomar los últimos N intervalos
        val recentIntervals = intervals.takeLast(expectedPattern.size - 1)
        
        // Comparar con tolerancia del 30%
        for (i in recentIntervals.indices) {
            val expected = expectedPattern[i].toLong()
            val actual = recentIntervals[i]
            val tolerance = expected * 0.3
            
            if (abs(actual - expected) > tolerance) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * COMANDOS ESPECIALES PARA SITUACIONES CRÍTICAS
     */
    fun detectEmergencyPanic(): Boolean {
        // Detector de pánico: sonido continuo fuerte por más de 3 segundos
        return false // Implementar según necesidades específicas
    }
    
    fun detectDistressSignal(): Boolean {
        // Detector de señal SOS: patrón específico de golpes
        return false // Implementar según necesidades específicas
    }
}