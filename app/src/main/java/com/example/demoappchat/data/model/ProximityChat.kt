package com.example.demoappchat.data.model

import com.google.firebase.database.PropertyName

data class ProximityChat(
    var id: String = "",
    var creatorId: String = "",
    var creatorName: String = "",
    var title: String = "",
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var radius: Int = 4000,
    var pin: String = "",
    var createdAt: Long = 0L,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    var participantsCount: Int = 0,
    var lastActivity: Long = 0L,
    var category: String = "emergency"
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", "", 0.0, 0.0, 4000, "", 0L, true, 0, 0L, "emergency")
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "creatorId" to creatorId,
            "creatorName" to creatorName,
            "title" to title,
            "description" to description,
            "latitude" to latitude,
            "longitude" to longitude,
            "radius" to radius,
            "pin" to pin,
            "createdAt" to createdAt,
            "isActive" to isActive,
            "participantsCount" to participantsCount,
            "lastActivity" to lastActivity,
            "category" to category
        )
    }

//    fun getDistanceText(userLat: Double, userLng: Double): String {
//        val distance = calculateDistance(latitude, longitude, userLat, userLng)
//        return when {
//            distance < 1000 -> "${distance.toInt()}m"
//            else -> "${"%.1f".format(distance / 1000)}km"
//        }
//    }

    fun getDistanceText(userLat: Double, userLng: Double): String {
        return "–" // o "N/A", "No disponible", "Sin calcular", etc.
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}