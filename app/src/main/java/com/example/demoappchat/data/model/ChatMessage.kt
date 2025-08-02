package com.example.demoappchat.data.model

data class ChatMessage(
    var id: String = "",
    var chatId: String = "",
    var userId: String = "",
    var userName: String = "",
    var userPhotoUrl: String = "",
    var messageType: MessageType = MessageType.TEXT,
    var content: String = "",
    var mediaUrl: String? = null,
    var timestamp: Long = 0L,
    var isDeleted: Boolean = false,
    var replyTo: String? = null
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", "", MessageType.TEXT, "", null, 0L, false, null)
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