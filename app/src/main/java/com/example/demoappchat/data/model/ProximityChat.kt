package com.example.demoappchat.data.model


data class ProximityChat(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Int = 4000,
    val pin: String = "",
    val createdAt: Long = 0L,
    val isActive: Boolean = true,
    val participantsCount: Int = 0,
    val lastActivity: Long = 0L,
    val category: String = "emergency" // emergency, security, traffic, community
) {
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