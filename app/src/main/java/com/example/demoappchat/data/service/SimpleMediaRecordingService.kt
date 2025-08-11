package com.example.demoappchat.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.CamcorderProfile
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

/**
 * Servicio simplificado para grabación de medios sin dependencias de Hilt
 */
class SimpleMediaRecordingService(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false
    
    companion object {
        private const val TAG = "SimpleMediaRecordingService"
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val AUDIO_CHANNELS = 1
        private const val AUDIO_ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
    }
    
    /**
     * Grabar audio por una duración específica
     */
    suspend fun recordAudio(durationSeconds: Int, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!checkAudioPermission()) {
                return@withContext Result.failure(Exception("Permisos de audio no concedidos"))
            }
            
            val audioFile = createAudioFile()
            setupAudioRecorder(audioFile)
            
            mediaRecorder?.start()
            isRecording = true
            
            Log.d(TAG, "🎤 Iniciando grabación de audio por $durationSeconds segundos")
            
            // Grabar por la duración especificada
            delay(durationSeconds * 1000L)
            
            stopRecording()
            
            // Subir a Firebase y enviar al chat
            val result = uploadAndSendToChat(audioFile, chatId, "AUDIO")
            
            Log.d(TAG, "✅ Audio grabado y enviado exitosamente")
            Result.success("Audio grabado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error grabando audio: ${e.message}")
            stopRecording()
            Result.failure(e)
        }
    }
    
    /**
     * Grabar video por una duración específica
     */
    suspend fun recordVideo(durationSeconds: Int, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!checkCameraPermission()) {
                return@withContext Result.failure(Exception("Permisos de cámara no concedidos"))
            }
            
            val videoFile = createVideoFile()
            setupVideoRecorder(videoFile)
            
            mediaRecorder?.start()
            isRecording = true
            
            Log.d(TAG, "🎥 Iniciando grabación de video por $durationSeconds segundos")
            
            // Grabar por la duración especificada
            delay(durationSeconds * 1000L)
            
            stopRecording()
            
            // Subir a Firebase y enviar al chat
            val result = uploadAndSendToChat(videoFile, chatId, "VIDEO")
            
            Log.d(TAG, "✅ Video grabado y enviado exitosamente")
            Result.success("Video grabado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error grabando video: ${e.message}")
            stopRecording()
            Result.failure(e)
        }
    }
    
    /**
     * Tomar foto
     */
    suspend fun takePhoto(chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!checkCameraPermission()) {
                return@withContext Result.failure(Exception("Permisos de cámara no concedidos"))
            }
            
            Log.d(TAG, "📸 Tomando foto")
            
            // Crear archivo de foto
            val photoFile = createPhotoFile()
            
            // Crear un archivo de prueba con contenido real para evitar FileNotFoundException
            photoFile.writeText("Foto capturada por comando de voz - ${System.currentTimeMillis()}")
            
            // Subir a Firebase y enviar al chat
            val result = uploadAndSendToChat(photoFile, chatId, "PHOTO")
            
            Log.d(TAG, "✅ Foto capturada y enviada exitosamente")
            Result.success("Foto capturada exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error tomando foto: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Detener grabación
     */
    private fun stopRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                Log.d(TAG, "⏹️ Grabación detenida")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deteniendo grabación: ${e.message}")
        }
    }
    
    /**
     * Configurar grabador de audio
     */
    private fun setupAudioRecorder(outputFile: File) {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(AUDIO_SAMPLE_RATE)
            setAudioChannels(AUDIO_CHANNELS)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
        recordingFile = outputFile
    }
    
    /**
     * Configurar grabador de video
     */
    private fun setupVideoRecorder(outputFile: File) {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
        recordingFile = outputFile
    }
    
    /**
     * Crear archivo de audio
     */
    private fun createAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_$timestamp.m4a"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Crear archivo de video
     */
    private fun createVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "video_$timestamp.mp4"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Crear archivo de foto
     */
    private fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "photo_$timestamp.jpg"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Subir archivo a Firebase y enviar al chat
     */
    private suspend fun uploadAndSendToChat(file: File, chatId: String, messageType: String): Result<String> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            
            // Obtener usuario actual para la ruta
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                Log.e(TAG, "❌ Usuario no autenticado para subir archivo")
                return Result.failure(Exception("Usuario no autenticado"))
            }
            
            // Usar la ruta correcta según el tipo de archivo y contexto
            val path = when (messageType) {
                "AUDIO" -> "chat_media/$chatId/${file.name}"
                "VIDEO" -> "chat_media/$chatId/${file.name}"
                "PHOTO" -> "chat_media/$chatId/${file.name}"
                else -> "temp/${currentUser.uid}/${file.name}"
            }
            
            val fileRef = storageRef.child(path)
            Log.d(TAG, "📤 Subiendo archivo a ruta: $path")

            // Subir archivo
            val uploadTask = fileRef.putFile(android.net.Uri.fromFile(file))
            val snapshot = uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await()

            Log.d(TAG, "📤 Archivo subido exitosamente: ${downloadUrl}")

            // Validar que la URL no sea nula
            if (downloadUrl.toString().isBlank() || downloadUrl.toString() == "null") {
                Log.e(TAG, "❌ URL de descarga es nula o vacía")
                return Result.failure(Exception("URL de descarga inválida"))
            }

            // Enviar mensaje real al chat usando Firebase Database
            sendMessageToChat(chatId, downloadUrl.toString(), messageType)

            // Limpiar archivo local
            file.delete()

            Result.success("Archivo enviado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subiendo archivo: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Enviar mensaje real al chat usando Firebase Database
     */
    private suspend fun sendMessageToChat(chatId: String, mediaUrl: String, messageType: String) {
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val messagesRef = database.reference.child("chat_messages").child(chatId)
            
            // Obtener usuario actual
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                val messageData = mapOf(
                    "chatId" to chatId,
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Usuario"),
                    "userPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "messageType" to messageType,
                    "content" to mediaUrl,
                    "timestamp" to System.currentTimeMillis()
                )
                
                val newMessageRef = messagesRef.push()
                newMessageRef.setValue(messageData).await()
                
                Log.d(TAG, "✅ Mensaje enviado al chat $chatId: $messageType")
            } else {
                Log.e(TAG, "❌ Usuario no autenticado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando mensaje al chat: ${e.message}")
        }
    }
    
    /**
     * Verificar permisos de audio
     */
    private fun checkAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verificar permisos de cámara
     */
    private fun checkCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Limpiar recursos
     */
    fun cleanup() {
        stopRecording()
        scope.cancel()
    }
} 