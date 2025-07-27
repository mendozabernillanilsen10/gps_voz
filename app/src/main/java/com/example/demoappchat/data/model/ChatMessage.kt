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
    var isDeleted: Boolean = false, // var para Firebase
    val replyTo: String? = null
) {
    // Constructor vacío requerido por Firebase
    constructor() : this(
        id = "",
        chatId = "",
        userId = "",
        userName = "",
        userPhotoUrl = "",
        messageType = MessageType.TEXT,
        content = "",
        mediaUrl = null,
        timestamp = 0L,
        isDeleted = false,
        replyTo = null
    )
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