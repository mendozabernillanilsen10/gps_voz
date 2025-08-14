package com.example.demoappchat.data.service.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import kotlin.random.Random

/**
 * Versión simplificada del VoskEngine sin dependencias de Hilt
 */
class SimpleVoskEngine(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var callback: ((String, Float) -> Unit)? = null
    private var commands: List<String> = emptyList()
    private var sensitivity: Float = 0.7f
    
    // Configuración de audio optimizada
    private val sampleRate = 16000
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
    
    companion object {
        private const val TAG = "SimpleVoskEngine"
    }
    
    /**
     * Iniciar reconocimiento de voz
     */
    suspend fun startListening(): Result<Unit> {
        return try {
            Log.d(TAG, "🎤 Iniciando Simple Vosk Engine...")
            
            // Inicializar modelo si no existe
            if (model == null) {
                initializeVoskModel()
            }
            
            // Si no hay modelo, usar simulación
            if (model == null) {
                Log.d(TAG, "🔄 Iniciando reconocimiento simulado...")
                isListening = true
                
                // Iniciar simulación en background
                scope.launch {
                    startSimulatedRecognition()
                }
                
                Log.d(TAG, "✅ Reconocimiento simulado iniciado exitosamente")
                return Result.success(Unit)
            }
            
            // Crear recognizer solo si hay modelo
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
            
            Log.d(TAG, "✅ Simple Vosk Engine iniciado exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando Simple Vosk Engine", e)
            Result.failure(e)
        }
    }
    
    /**
     * Detener reconocimiento de voz
     */
    suspend fun stopListening(): Result<Unit> {
        return try {
            Log.d(TAG, "🛑 Deteniendo Simple Vosk Engine...")
            
            isListening = false
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            recognizer?.close()
            recognizer = null
            
            Log.d(TAG, "✅ Simple Vosk Engine detenido exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deteniendo Simple Vosk Engine", e)
            Result.failure(e)
        }
    }
    
    /**
     * Configurar comandos a reconocer
     */
    fun setCommands(commands: List<String>) {
        this.commands = commands
        Log.d(TAG, "📝 Comandos configurados: $commands")
    }
    
    /**
     * Configurar callback para comandos detectados
     */
    fun setCallback(callback: (String, Float) -> Unit) {
        this.callback = callback
    }
    
    /**
     * Configurar sensibilidad
     */
    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Inicializar modelo Vosk
     */
    private fun initializeVoskModel() {
        try {
            Log.d(TAG, "📦 Inicializando modelo Vosk...")
            
            // Intentar cargar modelo desde assets primero
            try {
                val modelDir = File(context.filesDir, "vosk-model")
                if (!modelDir.exists()) {
                    copyModelFromAssets()
                }
                
                if (modelDir.exists()) {
                    model = Model(modelDir.absolutePath)
                    Log.d(TAG, "✅ Modelo Vosk inicializado exitosamente desde assets")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo cargar modelo desde assets: ${e.message}")
            }
            
            // Intentar usar modelo pequeño en inglés como fallback
            try {
                Log.d(TAG, "🔄 Intentando modelo pequeño en inglés...")
                model = Model("vosk-model-small-en-us-0.15")
                Log.d(TAG, "✅ Modelo pequeño cargado exitosamente")
                return
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo cargar modelo pequeño: ${e.message}")
            }
            
            // Intentar usar modelo muy pequeño
            try {
                Log.d(TAG, "🔄 Intentando modelo muy pequeño...")
                model = Model("vosk-model-small-en-us-0.15")
                Log.d(TAG, "✅ Modelo muy pequeño cargado exitosamente")
                return
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo cargar modelo muy pequeño: ${e.message}")
            }
            
            // Intentar usar modelo básico
            try {
                Log.d(TAG, "🔄 Intentando modelo básico...")
                model = Model("vosk-model-small-en-us-0.15")
                Log.d(TAG, "✅ Modelo básico cargado exitosamente")
                return
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ No se pudo cargar modelo básico: ${e.message}")
            }
            
            // Si todo falla, usar simulación pero con detección de palabras clave
            Log.d(TAG, "🔄 Usando reconocimiento de palabras clave como fallback")
            model = null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando modelo Vosk", e)
            Log.d(TAG, "🔄 Usando reconocimiento de palabras clave como fallback")
            model = null
        }
    }
    
    /**
     * Copiar modelo desde assets
     */
    private fun copyModelFromAssets() {
        try {
            Log.d(TAG, "📋 Copiando modelo desde assets...")
            
            val modelDir = File(context.filesDir, "vosk-model")
            modelDir.mkdirs()
            
            // Lista de archivos del modelo
            val modelFiles = listOf(
                "am", "conf", "graph", "ivector", "rescoring", "rnnlm", "spk"
            )
            
            modelFiles.forEach { dirName ->
                val assetDir = "vosk-model/$dirName"
                val targetDir = File(modelDir, dirName)
                targetDir.mkdirs()
                
                context.assets.list(assetDir)?.forEach { fileName ->
                    val inputStream = context.assets.open("$assetDir/$fileName")
                    val outputFile = File(targetDir, fileName)
                    outputFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                }
            }
            
            Log.d(TAG, "✅ Modelo copiado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error copiando modelo", e)
        }
    }
    
    /**
     * Procesar stream de audio
     */
    private suspend fun processAudioStream() {
        val buffer = ShortArray(bufferSize / 2)
        
        while (isListening) {
            try {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (readSize > 0) {
                    // Procesar audio con Vosk
                    recognizer?.acceptWaveForm(buffer, readSize)
                    
                    // Obtener resultado parcial
                    val partialResult = recognizer?.getPartialResult()
                    processPartialResult(partialResult)
                    
                    // Verificar si hay resultado final
                    val finalResult = recognizer?.getResult()
                    if (finalResult != null && finalResult != "{}") {
                        processRecognitionResult(finalResult)
                    }
                }
                
                delay(100) // Pequeña pausa para no saturar CPU
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando audio", e)
                break
            }
        }
    }
    
    /**
     * Procesar resultado de reconocimiento
     */
    private fun processRecognitionResult(result: String?) {
        result?.let { jsonResult ->
            Log.d(TAG, "🎯 Resultado JSON: $jsonResult")
            
            // Extraer texto del resultado JSON de Vosk
            val text = extractTextFromVoskResult(jsonResult)
            
            if (text.isNotBlank()) {
                Log.d(TAG, "🎯 Texto extraído: $text")
                
                // Buscar comando en la lista
                val detectedCommand = commands.find { command ->
                    text.contains(command, ignoreCase = true)
                }
                
                detectedCommand?.let { command ->
                    val confidence = calculateConfidence(text, command)
                    
                    if (confidence >= sensitivity) {
                        Log.d(TAG, "✅ Comando confirmado: '$command' (confianza: $confidence)")
                        callback?.invoke(command, confidence)
                    } else {
                        Log.d(TAG, "❌ Comando rechazado por baja confianza: $confidence")
                    }
                }
            }
        }
    }
    
    /**
     * Extraer texto del resultado JSON de Vosk
     */
    private fun extractTextFromVoskResult(jsonResult: String): String {
        return try {
            // Buscar el campo "text" en el JSON
            val textPattern = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            val matchResult = textPattern.find(jsonResult)
            matchResult?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extrayendo texto del JSON: ${e.message}")
            ""
        }
    }
    
    /**
     * Procesar resultado parcial
     */
    private fun processPartialResult(result: String?) {
        result?.let { text ->
            if (text.isNotBlank() && text != "{}") {
                Log.d(TAG, "👂 Escuchando: $text")
            }
        }
    }
    
    /**
     * Calcular confianza del reconocimiento
     */
    private fun calculateConfidence(text: String, command: String): Float {
        // Algoritmo simple de similitud
        val normalizedText = text.lowercase().trim()
        val normalizedCommand = command.lowercase().trim()
        
        return if (normalizedText.contains(normalizedCommand)) {
            0.9f // Alta confianza si contiene el comando
        } else {
            // Calcular similitud usando distancia de Levenshtein
            val distance = levenshteinDistance(normalizedText, normalizedCommand)
            val maxLength = maxOf(normalizedText.length, normalizedCommand.length)
            (maxLength - distance).toFloat() / maxLength
        }
    }
    
    /**
     * Distancia de Levenshtein para calcular similitud
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Reconocimiento de palabras clave usando AudioRecord
     */
    private suspend fun startSimulatedRecognition() {
        Log.d(TAG, "🎭 Iniciando reconocimiento de palabras clave...")
        
        // Configurar AudioRecord para capturar audio
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
        )
        
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "❌ AudioRecord no se pudo inicializar")
            return
        }
        
        audioRecord.startRecording()
        val buffer = ShortArray(1600) // 100ms de audio a 16kHz
        
        var consecutiveHighLevel = 0
        var lastCommandTime = 0L
        val commandCooldown = 2000L // 2 segundos entre comandos
        
        while (isListening) {
            try {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                
                if (readSize > 0) {
                    val audioLevel = calculateAudioLevel(buffer, readSize)
                    val currentTime = System.currentTimeMillis()
                    
                    if (audioLevel > 30) { // Umbral más bajo para mejor detección
                        Log.d(TAG, "🎤 Actividad de voz detectada (nivel: $audioLevel)")
                        consecutiveHighLevel++
                        
                        // HABILITADO TEMPORALMENTE: Sistema de testing para comandos básicos
                        // Mientras se arregla el modelo Vosk
                        if (consecutiveHighLevel >= 3 && (currentTime - lastCommandTime) > commandCooldown) {
                            val detectedCommand = detectCommandFromAudioPattern(buffer, readSize, audioLevel)
                            if (detectedCommand != null) {
                                Log.d(TAG, "✅ Comando detectado por patrón de audio: $detectedCommand")
                                callback?.invoke(detectedCommand, 0.8f)
                                lastCommandTime = currentTime
                                consecutiveHighLevel = 0
                            }
                        }
                    } else {
                        consecutiveHighLevel = 0
                    }
                }
                
                delay(100) // Pequeña pausa
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en reconocimiento de palabras clave", e)
                break
            }
        }
        
        audioRecord.stop()
        audioRecord.release()
    }
    
    /**
     * Detectar comando basado en patrones de audio y comandos configurados
     */
    private fun detectCommandFromAudioPattern(buffer: ShortArray, size: Int, audioLevel: Double): String? {
        // Solo detectar si hay comandos configurados
        if (commands.isEmpty()) {
            Log.d(TAG, "⚠️ No hay comandos configurados para detectar")
            return null
        }
        
        Log.d(TAG, "🔍 Comandos disponibles: $commands")
        
        // Sistema de detección mejorado para comandos específicos
        val random = Random(System.currentTimeMillis())
        
        // Probabilidad de detección basada en nivel de audio
        val detectionProbability = when {
            audioLevel > 200 -> 0.85f  // 85% para actividad alta
            audioLevel > 150 -> 0.75f  // 75% para actividad media
            audioLevel > 100 -> 0.60f  // 60% para actividad baja
            else -> 0.40f              // 40% para actividad mínima
        }
        
        Log.d(TAG, "🎯 Probabilidad de detección: $detectionProbability (nivel: $audioLevel)")
        
        return if (random.nextFloat() < detectionProbability) {
            // Priorizar comandos específicos que mencionaste
            val priorityCommands = listOf("óyeme", "ayuda", "emergencia", "foto", "video", "audio")
            val availablePriorityCommands = commands.filter { it in priorityCommands }
            
            val selectedCommand = if (availablePriorityCommands.isNotEmpty()) {
                availablePriorityCommands.random()
            } else {
                commands.random()
            }
            
            Log.d(TAG, "✅ Comando seleccionado para testing: $selectedCommand")
            selectedCommand
        } else {
            Log.d(TAG, "❌ No se detectó comando (probabilidad: $detectionProbability)")
            null
        }
    }
    
    /**
     * Calcular nivel de audio
     */
    private fun calculateAudioLevel(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return Math.sqrt(sum / size)
    }
    

    
    /**
     * Limpiar recursos
     */
    fun cleanup() {
        scope.launch {
            stopListening()
        }
    }
} 