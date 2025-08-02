package com.example.demoappchat.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para manejo de ubicación y servicios GPS
 */
interface LocationRepository {
    
    /**
     * Obtiene la ubicación actual del dispositivo
     */
    suspend fun getCurrentLocation(): Flow<LocationData>
    
    /**
     * Inicia seguimiento de ubicación en tiempo real
     */
    suspend fun startLocationTracking(): Result<Unit>
    
    /**
     * Detiene seguimiento de ubicación
     */
    suspend fun stopLocationTracking(): Result<Unit>
    
    /**
     * Verifica si el seguimiento está activo
     */
    fun isLocationTrackingActive(): Flow<Boolean>
    
    /**
     * Obtiene historial de ubicaciones
     */
    fun getLocationHistory(): Flow<List<LocationData>>
    
    /**
     * Calcula distancia entre dos puntos
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float
    
    /**
     * Verifica si los servicios de ubicación están habilitados
     */
    suspend fun isLocationEnabled(): Boolean
    
    /**
     * Solicita habilitar servicios de ubicación
     */
    suspend fun requestLocationServices(): Result<Unit>
}

/**
 * Datos de ubicación
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val address: String? = null,
    val speed: Float = 0f,
    val bearing: Float = 0f
)