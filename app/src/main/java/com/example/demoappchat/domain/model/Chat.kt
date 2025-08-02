package com.example.demoappchat.domain.model

/**
 * Modelos de dominio para Chat
 */
data class Chat(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val pin: String,
    val participantsCount: Int,
    val isActive: Boolean,
    val category: ChatCategory,
    val createdAt: Long,
    val lastActivity: Long
)

data class Message(
    val id: String,
    val chatId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String,
    val type: MessageType,
    val content: String,
    val mediaUrl: String?,
    val timestamp: Long,
    val isDeleted: Boolean,
    val replyTo: String?
)

enum class ChatCategory {
    EMERGENCY, SECURITY, SURVEILLANCE, GENERAL
}

enum class MessageType {
    TEXT, PHOTO, VIDEO, AUDIO, LOCATION, SYSTEM, VOICE_COMMAND
}