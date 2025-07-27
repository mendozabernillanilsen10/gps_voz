package com.example.demoappchat.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastSeen: Long = 0L,
    var fcmToken: String = "", // var para Firebase
    var isActive: Boolean = true // var para Firebase
) {
    // Constructor vacío requerido por Firebase
    constructor() : this(
        id = "",
        name = "",
        email = "",
        photoUrl = "",
        latitude = 0.0,
        longitude = 0.0,
        lastSeen = 0L,
        fcmToken = "",
        isActive = true
    )
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

