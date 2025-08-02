package com.example.demoappchat.data.model

data class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var lastSeen: Long = 0L,
    var fcmToken: String = "",
    var isActive: Boolean = true
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", 0.0, 0.0, 0L, "", true)
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "email" to email,
            "photoUrl" to photoUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "lastSeen" to lastSeen,
            "fcmToken" to fcmToken,
            "isActive" to isActive
        )
    }
}

