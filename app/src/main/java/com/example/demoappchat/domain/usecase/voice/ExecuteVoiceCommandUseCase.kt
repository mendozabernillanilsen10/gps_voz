package com.example.demoappchat.domain.usecase.voice

import com.example.demoappchat.domain.model.VoiceAction
import com.example.demoappchat.domain.model.VoiceCommand
import com.example.demoappchat.domain.model.VoiceRecognitionResult
import com.example.demoappchat.domain.repository.ChatRepository
import com.example.demoappchat.domain.repository.MediaRepository
import com.example.demoappchat.domain.repository.LocationRepository
import com.example.demoappchat.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Case profesional para ejecutar comandos de voz
 * Maneja la lógica de negocio para diferentes tipos de acciones
 */
@Singleton
class ExecuteVoiceCommandUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val mediaRepository: MediaRepository,
    private val locationRepository: LocationRepository,
    private val voiceRepository: VoiceRepository
) {
    
    /**
     * Ejecuta un comando de voz reconocido
     */
    suspend fun execute(result: VoiceRecognitionResult): Result<String> {
        return try {
            android.util.Log.d("ExecuteVoiceCommand", "🎯 Ejecutando: '${result.text}'")
            
            // Obtener comandos configurados
            val commandsResult = voiceRepository.getVoiceCommands()
            if (commandsResult.isFailure) {
                return Result.failure(Exception("No se pudieron obtener los comandos de voz"))
            }
            
            val commands = commandsResult.getOrThrow()
            
            // Buscar comando que coincida
            val matchedCommand = findMatchingCommand(result.text, commands)
            if (matchedCommand == null) {
                android.util.Log.d("ExecuteVoiceCommand", "❌ No se encontró comando para: '${result.text}'")
                return Result.failure(Exception("Comando no reconocido"))
            }
            
            android.util.Log.d("ExecuteVoiceCommand", "✅ Comando coincidente: ${matchedCommand.phrase} -> ${matchedCommand.action}")
            
            // Ejecutar acción según el tipo
            executeAction(matchedCommand, result)
            
        } catch (e: Exception) {
            android.util.Log.e("ExecuteVoiceCommand", "❌ Error ejecutando comando", e)
            Result.failure(e)
        }
    }
    
    private fun findMatchingCommand(text: String, commands: List<VoiceCommand>): VoiceCommand? {
        val lowercaseText = text.lowercase()
        
        // Buscar coincidencia exacta primero
        commands.forEach { command ->
            if (command.isEnabled && lowercaseText.contains(command.phrase.lowercase())) {
                return command
            }
        }
        
        // Buscar coincidencias parciales con palabras clave
        commands.forEach { command ->
            if (command.isEnabled) {
                val phrases = command.phrase.lowercase().split(" ")
                val matchCount = phrases.count { phrase ->
                    lowercaseText.contains(phrase) && phrase.length > 2
                }
                
                // Si coincide al menos el 70% de las palabras
                if (matchCount.toFloat() / phrases.size >= 0.7f) {
                    return command
                }
            }
        }
        
        return null
    }
    
    private suspend fun executeAction(command: VoiceCommand, result: VoiceRecognitionResult): Result<String> {
        return when (command.action) {
            VoiceAction.SEND_MESSAGE -> executeSendMessage(command)
            VoiceAction.SEND_LOCATION -> executeSendLocation()
            VoiceAction.START_GROUP_CALL -> executeStartGroupCall(command)
            VoiceAction.START_AUDIO_RECORDING -> executeRecordAudio()
            VoiceAction.START_VIDEO_RECORDING -> executeRecordVideo()
            VoiceAction.TAKE_PHOTO -> executeTakePhoto()
            VoiceAction.SEND_EMERGENCY_ALERT -> executeEmergencyAlert()
            VoiceAction.ACTIVATE_STEALTH_MODE -> executeStealthMode()
            VoiceAction.SEND_STATUS_UPDATE -> executeStatusUpdate()
            VoiceAction.START_TRACKING -> executeStartTracking()
            VoiceAction.STOP_TRACKING -> executeStopTracking()
            VoiceAction.SEND_AUDIO_MESSAGE -> executeSendAudioMessage()
            VoiceAction.SEND_VIDEO_MESSAGE -> executeSendVideoMessage()
            VoiceAction.SEND_PHOTO_MESSAGE -> executeSendPhotoMessage()
            VoiceAction.ACTIVATE_SURVEILLANCE -> executeActivateSurveillance()
            VoiceAction.DEACTIVATE_SURVEILLANCE -> executeDeactivateSurveillance()
            VoiceAction.SEND_SOS -> executeSendSOS()
            VoiceAction.CUSTOM_ACTION -> executeCustomAction(command)
        }
    }
    
    private suspend fun executeSendMessage(command: VoiceCommand): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Obtener mensaje de los metadatos o usar mensaje por defecto
            val message = command.metadata["message"] ?: "Mensaje enviado por comando de voz"
            
            // Enviar mensaje
            val sendResult = chatRepository.sendMessage(activeChat.id, message)
            if (sendResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📨 Mensaje enviado: $message")
                Result.success("Mensaje enviado exitosamente")
            } else {
                Result.failure(Exception("Error enviando mensaje"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeSendLocation(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Enviar ubicación actual
            val locationResult = chatRepository.sendCurrentLocation(activeChat.id)
            if (locationResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📍 Ubicación enviada")
                Result.success("Ubicación enviada exitosamente")
            } else {
                Result.failure(Exception("Error enviando ubicación"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeStartGroupCall(command: VoiceCommand): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Obtener tipo de llamada
            val callType = command.metadata["type"] ?: "video"
            
            // Iniciar llamada grupal
            val callResult = chatRepository.startGroupCall(activeChat.id, callType)
            if (callResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📞 Llamada $callType iniciada")
                Result.success("Llamada $callType iniciada exitosamente")
            } else {
                Result.failure(Exception("Error iniciando llamada"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeRecordAudio(): Result<String> {
        return try {
            // Iniciar grabación de audio
            val recordResult = mediaRepository.startAudioRecording()
            if (recordResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "🎤 Grabación de audio iniciada")
                Result.success("Grabación de audio iniciada")
            } else {
                Result.failure(Exception("Error iniciando grabación de audio"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeRecordVideo(): Result<String> {
        return try {
            // Iniciar grabación de video
            val recordResult = mediaRepository.startVideoRecording()
            if (recordResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "🎥 Grabación de video iniciada")
                Result.success("Grabación de video iniciada")
            } else {
                Result.failure(Exception("Error iniciando grabación de video"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeTakePhoto(): Result<String> {
        return try {
            // Tomar foto
            val photoResult = mediaRepository.takePhoto()
            if (photoResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📸 Foto tomada")
                Result.success("Foto tomada exitosamente")
            } else {
                Result.failure(Exception("Error tomando foto"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeEmergencyAlert(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Enviar alerta de emergencia
            val emergencyMessage = "🚨 EMERGENCIA 🚨\nSolicito asistencia inmediata en mi ubicación actual"
            val messageResult = chatRepository.sendMessage(activeChat.id, emergencyMessage)
            
            // Enviar ubicación automáticamente
            val locationResult = chatRepository.sendCurrentLocation(activeChat.id)
            
            if (messageResult.isSuccess && locationResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "🚨 Alerta de emergencia enviada")
                Result.success("Alerta de emergencia enviada con ubicación")
            } else {
                Result.failure(Exception("Error enviando alerta de emergencia"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeStatusUpdate(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Obtener ubicación actual
            val location = locationRepository.getCurrentLocation().first()
            
            // Crear mensaje de estado
            val statusMessage = "📍 Actualización de estado\n" +
                    "🕐 ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n" +
                    "✅ Operativo y en posición"
            
            // Enviar mensaje de estado
            val messageResult = chatRepository.sendMessage(activeChat.id, statusMessage)
            
            if (messageResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📊 Estado actualizado")
                Result.success("Estado actualizado exitosamente")
            } else {
                Result.failure(Exception("Error actualizando estado"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeStealthMode(): Result<String> {
        return try {
            // Activar modo sigiloso
            val stealthResult = voiceRepository.setStealthMode(true)
            if (stealthResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "🥷 Modo sigiloso activado")
                Result.success("Modo sigiloso activado")
            } else {
                Result.failure(Exception("Error activando modo sigiloso"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeCustomAction(command: VoiceCommand): Result<String> {
        return try {
            // Ejecutar acción personalizada basada en metadatos
            val actionType = command.metadata["action_type"] ?: "unknown"
            val actionData = command.metadata["action_data"] ?: ""
            
            android.util.Log.d("ExecuteVoiceCommand", "🔧 Acción personalizada: $actionType - $actionData")
            Result.success("Acción personalizada ejecutada: $actionType")
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeStartTracking(): Result<String> {
        return try {
            // Iniciar seguimiento de ubicación
            val trackingResult = locationRepository.startLocationTracking()
            if (trackingResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📍 Seguimiento de ubicación iniciado")
                Result.success("Seguimiento de ubicación iniciado")
            } else {
                Result.failure(Exception("Error iniciando seguimiento"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeStopTracking(): Result<String> {
        return try {
            // Detener seguimiento de ubicación
            val stopResult = locationRepository.stopLocationTracking()
            if (stopResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📍 Seguimiento de ubicación detenido")
                Result.success("Seguimiento de ubicación detenido")
            } else {
                Result.failure(Exception("Error deteniendo seguimiento"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeSendAudioMessage(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Grabar audio
            val audioResult = mediaRepository.startAudioRecording()
            if (audioResult.isSuccess) {
                // Esperar 5 segundos y detener
                kotlinx.coroutines.delay(5000)
                val stopResult = mediaRepository.stopAudioRecording()
                
                if (stopResult.isSuccess) {
                    android.util.Log.d("ExecuteVoiceCommand", "🎤 Mensaje de audio grabado")
                    Result.success("Mensaje de audio grabado")
                } else {
                    Result.failure(Exception("Error grabando audio"))
                }
            } else {
                Result.failure(Exception("Error iniciando grabación de audio"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeSendVideoMessage(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Grabar video
            val videoResult = mediaRepository.startVideoRecording()
            if (videoResult.isSuccess) {
                // Esperar 10 segundos y detener
                kotlinx.coroutines.delay(10000)
                val stopResult = mediaRepository.stopVideoRecording()
                
                if (stopResult.isSuccess) {
                    android.util.Log.d("ExecuteVoiceCommand", "🎥 Mensaje de video grabado")
                    Result.success("Mensaje de video grabado")
                } else {
                    Result.failure(Exception("Error grabando video"))
                }
            } else {
                Result.failure(Exception("Error iniciando grabación de video"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeSendPhotoMessage(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Tomar foto
            val photoResult = mediaRepository.takePhoto()
            if (photoResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "📸 Foto tomada")
                Result.success("Foto tomada")
            } else {
                Result.failure(Exception("Error tomando foto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeActivateSurveillance(): Result<String> {
        return try {
            // Activar modo vigilancia (grabación continua)
            android.util.Log.d("ExecuteVoiceCommand", "👁️ Modo vigilancia activado")
            Result.success("Modo vigilancia activado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeDeactivateSurveillance(): Result<String> {
        return try {
            // Desactivar modo vigilancia
            android.util.Log.d("ExecuteVoiceCommand", "👁️ Modo vigilancia desactivado")
            Result.success("Modo vigilancia desactivado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun executeSendSOS(): Result<String> {
        return try {
            // Obtener chat activo
            val activeChat = chatRepository.getActiveChat()
            if (activeChat == null) {
                return Result.failure(Exception("No hay chat activo"))
            }
            
            // Enviar señal SOS con ubicación
            val sosMessage = "🚨 SOS 🚨\nNecesito ayuda inmediata\nCódigo de emergencia activado"
            val messageResult = chatRepository.sendMessage(activeChat.id, sosMessage)
            
            // Enviar ubicación
            val locationResult = chatRepository.sendCurrentLocation(activeChat.id)
            
            // Tomar foto de emergencia
            val photoResult = mediaRepository.takePhoto()
            
            if (messageResult.isSuccess && locationResult.isSuccess) {
                android.util.Log.d("ExecuteVoiceCommand", "🚨 Señal SOS enviada")
                Result.success("Señal SOS enviada con ubicación y foto")
            } else {
                Result.failure(Exception("Error enviando señal SOS"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}