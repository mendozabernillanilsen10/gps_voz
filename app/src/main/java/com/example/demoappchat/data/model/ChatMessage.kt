package com.example.demoappchat.data.model

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val content: String = "",
    val mediaUrl: String? = null,
    val timestamp: Long = 0L,
    val isDeleted: Boolean = false,
    val replyTo: String? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "chatId" to chatId,
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "messageType" to messageType.name,
            "content" to content,
            "mediaUrl" to (mediaUrl ?: ""),
            "timestamp" to timestamp,
            "isDeleted" to isDeleted,
            "replyTo" to (replyTo ?: "")
        )
    }
}

enum class MessageType {
    TEXT, PHOTO, VIDEO, AUDIO, LOCATION, SYSTEM
}