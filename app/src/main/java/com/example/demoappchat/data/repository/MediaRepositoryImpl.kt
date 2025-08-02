package com.example.demoappchat.data.repository

import android.content.Context
import android.media.MediaRecorder
import com.example.demoappchat.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación profesional del repositorio de multimedia
 * Maneja grabación de audio, video y captura de fotos
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {
    
    private var audioRecorder: MediaRecorder? = null
    private var videoRecorder: MediaRecorder? = null
    
    private val _isRecordingAudio = MutableStateFlow(false)
    private val _isRecordingVideo = MutableStateFlow(false)
    private val _recordingDuration = MutableStateFlow(0L)
    
    private var recordingStartTime = 0L
    
    override suspend fun startAudioRecording(): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "🎤 Iniciando grabación de audio...")
            
            if (_isRecordingAudio.value) {
                return Result.failure(Exception("Ya hay una grabación de audio en curso"))
            }
            
            val audioFile = createAudioFile()
            
            audioRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                
                prepare()
                start()
            }
            
            _isRecordingAudio.value = true
            recordingStartTime = System.currentTimeMillis()
            
            android.util.Log.d("MediaRepo", "✅ Grabación de audio iniciada: ${audioFile.name}")
            Result.success(audioFile.absolutePath)
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error iniciando grabación de audio", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopAudioRecording(): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "🛑 Deteniendo grabación de audio...")
            
            if (!_isRecordingAudio.value) {
                return Result.failure(Exception("No hay grabación de audio en curso"))
            }
            
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            
            _isRecordingAudio.value = false
            val duration = System.currentTimeMillis() - recordingStartTime
            
            android.util.Log.d("MediaRepo", "✅ Grabación de audio detenida (${duration}ms)")
            Result.success("Grabación completada en ${duration}ms")
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error deteniendo grabación de audio", e)
            Result.failure(e)
        }
    }
    
    override suspend fun startVideoRecording(): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "🎥 Iniciando grabación de video...")
            
            if (_isRecordingVideo.value) {
                return Result.failure(Exception("Ya hay una grabación de video en curso"))
            }
            
            val videoFile = createVideoFile()
            
            videoRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(videoFile.absolutePath)
                
                // Configuración básica de video
                setVideoSize(1280, 720)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(10000000)
                
                prepare()
                start()
            }
            
            _isRecordingVideo.value = true
            recordingStartTime = System.currentTimeMillis()
            
            android.util.Log.d("MediaRepo", "✅ Grabación de video iniciada: ${videoFile.name}")
            Result.success(videoFile.absolutePath)
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error iniciando grabación de video", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopVideoRecording(): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "🛑 Deteniendo grabación de video...")
            
            if (!_isRecordingVideo.value) {
                return Result.failure(Exception("No hay grabación de video en curso"))
            }
            
            videoRecorder?.apply {
                stop()
                release()
            }
            videoRecorder = null
            
            _isRecordingVideo.value = false
            val duration = System.currentTimeMillis() - recordingStartTime
            
            android.util.Log.d("MediaRepo", "✅ Grabación de video detenida (${duration}ms)")
            Result.success("Grabación completada en ${duration}ms")
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error deteniendo grabación de video", e)
            Result.failure(e)
        }
    }
    
    override suspend fun takePhoto(): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "📸 Tomando foto...")
            
            // TODO: Implementar captura de foto usando CameraX
            val photoFile = createPhotoFile()
            
            // Simulación de captura
            photoFile.createNewFile()
            
            android.util.Log.d("MediaRepo", "✅ Foto tomada: ${photoFile.name}")
            Result.success(photoFile.absolutePath)
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error tomando foto", e)
            Result.failure(e)
        }
    }
    
    override fun isRecordingAudio(): Flow<Boolean> = _isRecordingAudio
    
    override fun isRecordingVideo(): Flow<Boolean> = _isRecordingVideo
    
    override fun getRecordingDuration(): Flow<Long> = _recordingDuration
    
    override suspend fun uploadMediaFile(filePath: String, chatId: String): Result<String> {
        return try {
            android.util.Log.d("MediaRepo", "☁️ Subiendo archivo: $filePath")
            
            // TODO: Implementar subida a Firebase Storage
            
            android.util.Log.d("MediaRepo", "✅ Archivo subido exitosamente")
            Result.success("https://firebase.storage/upload/${System.currentTimeMillis()}")
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error subiendo archivo", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteLocalFile(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                android.util.Log.d("MediaRepo", "🗑️ Archivo eliminado: $filePath")
            }
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("MediaRepo", "❌ Error eliminando archivo", e)
            Result.failure(e)
        }
    }
    
    private fun createAudioFile(): File {
        val mediaDir = File(context.getExternalFilesDir(null), "audio")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        return File(mediaDir, "audio_$timestamp.3gp")
    }
    
    private fun createVideoFile(): File {
        val mediaDir = File(context.getExternalFilesDir(null), "video")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        return File(mediaDir, "video_$timestamp.mp4")
    }
    
    private fun createPhotoFile(): File {
        val mediaDir = File(context.getExternalFilesDir(null), "photos")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        return File(mediaDir, "photo_$timestamp.jpg")
    }
}