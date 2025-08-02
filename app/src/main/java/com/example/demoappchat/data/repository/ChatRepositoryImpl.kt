package com.example.demoappchat.data.repository

import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.model.ChatMessage
import com.example.demoappchat.data.model.MessageType
import com.example.demoappchat.domain.model.Chat
import com.example.demoappchat.domain.model.Message
import com.example.demoappchat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación profesional del repositorio de chat
 * Integra con Firebase y maneja estado local
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ChatRepository {
    
    private val _activeChat = MutableStateFlow<Chat?>(null)
    
    override suspend fun getActiveChat(): Chat? {
        return _activeChat.value
    }
    
    override suspend fun setActiveChat(chatId: String) {
        try {
            // TODO: Obtener chat de Firebase por ID
            android.util.Log.d("ChatRepo", "📱 Estableciendo chat activo: $chatId")
            
            // Por ahora crear un chat dummy
            val chat = Chat(
                id = chatId,
                title = "Chat Operativo",
                description = "Chat de operaciones policiales",
                creatorId = "current_user",
                creatorName = "Usuario Actual",
                latitude = 0.0,
                longitude = 0.0,
                radius = 1000,
                pin = "1234",
                participantsCount = 1,
                isActive = true,
                category = com.example.demoappchat.domain.model.ChatCategory.SECURITY,
                createdAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )
            
            _activeChat.value = chat
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "Error estableciendo chat activo", e)
        }
    }
    
    override suspend fun sendMessage(chatId: String, content: String): Result<String> {
        return try {
            android.util.Log.d("ChatRepo", "📨 Enviando mensaje: $content")
            
            // Crear ChatMessage con los datos necesarios
            val currentUser = firebaseRepository.currentUser.value 
                ?: throw Exception("Usuario no autenticado")
            
            val message = ChatMessage(
                chatId = chatId,
                userId = currentUser.id,
                userName = currentUser.name,
                content = content,
                messageType = MessageType.TEXT,
                timestamp = System.currentTimeMillis(),
                isDeleted = false
            )
            
            // Usar FirebaseRepository existente
            val result = firebaseRepository.sendMessage(message)
            
            if (result.isSuccess) {
                android.util.Log.d("ChatRepo", "✅ Mensaje enviado exitosamente")
                Result.success("Mensaje enviado")
            } else {
                android.util.Log.e("ChatRepo", "❌ Error enviando mensaje")
                Result.failure(Exception("Error enviando mensaje"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "❌ Excepción enviando mensaje", e)
            Result.failure(e)
        }
    }
    
    override suspend fun sendCurrentLocation(chatId: String): Result<String> {
        return try {
            android.util.Log.d("ChatRepo", "📍 Enviando ubicación actual")
            
            // Crear ChatMessage para ubicación
            val currentUser = firebaseRepository.currentUser.value 
                ?: throw Exception("Usuario no autenticado")
            
            val message = ChatMessage(
                chatId = chatId,
                userId = currentUser.id,
                userName = currentUser.name,
                content = "📍 Ubicación compartida",
                messageType = MessageType.LOCATION,
                timestamp = System.currentTimeMillis(),
                isDeleted = false
            )
            
            // TODO: Obtener ubicación actual e integrar con Firebase
            val result = firebaseRepository.sendMessage(message)
            
            if (result.isSuccess) {
                android.util.Log.d("ChatRepo", "✅ Ubicación enviada exitosamente")
                Result.success("Ubicación enviada")
            } else {
                Result.failure(Exception("Error enviando ubicación"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "❌ Excepción enviando ubicación", e)
            Result.failure(e)
        }
    }
    
    override suspend fun startGroupCall(chatId: String, type: String): Result<String> {
        return try {
            android.util.Log.d("ChatRepo", "📞 Iniciando llamada grupal: $type")
            
            // Usar FirebaseRepository para notificaciones de llamada
            val result = firebaseRepository.sendGroupCallNotification(chatId, type, "Usuario Actual")
            
            if (result.isSuccess) {
                android.util.Log.d("ChatRepo", "✅ Llamada grupal iniciada")
                Result.success("Llamada $type iniciada")
            } else {
                Result.failure(Exception("Error iniciando llamada"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "❌ Excepción iniciando llamada", e)
            Result.failure(e)
        }
    }
    
    override fun observeMessages(chatId: String): Flow<List<Message>> {
        // TODO: Implementar observación de mensajes de Firebase
        return MutableStateFlow(emptyList<Message>())
    }
    
    override fun observeNearbyChats(): Flow<List<Chat>> {
        // TODO: Implementar observación de chats cercanos
        return MutableStateFlow(emptyList<Chat>())
    }
    
    override suspend fun createProximityChat(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<String> {
        return try {
            android.util.Log.d("ChatRepo", "🏗️ Creando chat de proximidad: $title")
            
            // TODO: Integrar con FirebaseRepository para crear chat
            val chatId = "chat_${System.currentTimeMillis()}"
            
            // Establecer como chat activo
            setActiveChat(chatId)
            
            Result.success(chatId)
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "❌ Error creando chat", e)
            Result.failure(e)
        }
    }
    
    override suspend fun joinChatByPin(pin: String): Result<String> {
        return try {
            android.util.Log.d("ChatRepo", "🔐 Uniéndose a chat por PIN: $pin")
            
            // TODO: Buscar chat por PIN en Firebase
            val chatId = "chat_$pin"
            
            // Establecer como chat activo
            setActiveChat(chatId)
            
            Result.success(chatId)
            
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "❌ Error uniéndose a chat", e)
            Result.failure(e)
        }
    }
}