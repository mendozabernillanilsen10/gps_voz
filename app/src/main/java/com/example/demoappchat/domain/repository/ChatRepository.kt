package com.example.demoappchat.domain.repository

import com.example.demoappchat.domain.model.Chat
import com.example.demoappchat.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para manejo de chats y mensajes
 */
interface ChatRepository {
    
    /**
     * Obtiene el chat activo actual
     */
    suspend fun getActiveChat(): Chat?
    
    /**
     * Establece el chat activo
     */
    suspend fun setActiveChat(chatId: String)
    
    /**
     * Envía un mensaje al chat
     */
    suspend fun sendMessage(chatId: String, content: String): Result<String>
    
    /**
     * Envía la ubicación actual al chat
     */
    suspend fun sendCurrentLocation(chatId: String): Result<String>
    
    /**
     * Inicia una llamada grupal
     */
    suspend fun startGroupCall(chatId: String, type: String): Result<String>
    
    /**
     * Obtiene mensajes del chat
     */
    fun observeMessages(chatId: String): Flow<List<Message>>
    
    /**
     * Obtiene chats cercanos
     */
    fun observeNearbyChats(): Flow<List<Chat>>
    
    /**
     * Crea un nuevo chat de proximidad
     */
    suspend fun createProximityChat(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<String>
    
    /**
     * Se une a un chat por PIN
     */
    suspend fun joinChatByPin(pin: String): Result<String>
}