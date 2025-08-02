package com.example.demoappchat.data.repository

import android.content.Context
import android.location.LocationManager
import com.example.demoappchat.domain.repository.LocationRepository
import com.example.demoappchat.domain.repository.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Implementación profesional del repositorio de ubicación
 * Maneja GPS, seguimiento y cálculos geográficos
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableStateFlow(
        LocationData(
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            timestamp = System.currentTimeMillis()
        )
    )
    
    private val _isLocationTrackingActive = MutableStateFlow(false)
    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    
    override suspend fun getCurrentLocation(): Flow<LocationData> {
        return try {
            android.util.Log.d("LocationRepo", "📍 Obteniendo ubicación actual...")
            
            // TODO: Implementar obtención real de ubicación con FusedLocationProvider
            // Por ahora devolver ubicación simulada
            val simulatedLocation = LocationData(
                latitude = 4.6097, // Bogotá, Colombia (ejemplo)
                longitude = -74.0817,
                accuracy = 10f,
                timestamp = System.currentTimeMillis(),
                address = "Bogotá, Colombia",
                speed = 0f,
                bearing = 0f
            )
            
            _currentLocation.value = simulatedLocation
            flowOf(simulatedLocation)
            
        } catch (e: Exception) {
            android.util.Log.e("LocationRepo", "❌ Error obteniendo ubicación", e)
            flowOf(_currentLocation.value)
        }
    }
    
    override suspend fun startLocationTracking(): Result<Unit> {
        return try {
            android.util.Log.d("LocationRepo", "🎯 Iniciando seguimiento de ubicación...")
            
            if (!isLocationEnabled()) {
                return Result.failure(Exception("Servicios de ubicación deshabilitados"))
            }
            
            _isLocationTrackingActive.value = true
            
            // TODO: Implementar seguimiento real con LocationManager o FusedLocationProvider
            
            android.util.Log.d("LocationRepo", "✅ Seguimiento de ubicación iniciado")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("LocationRepo", "❌ Error iniciando seguimiento", e)
            Result.failure(e)
        }
    }
    
    override suspend fun stopLocationTracking(): Result<Unit> {
        return try {
            android.util.Log.d("LocationRepo", "🛑 Deteniendo seguimiento de ubicación...")
            
            _isLocationTrackingActive.value = false
            
            // TODO: Detener listeners de ubicación
            
            android.util.Log.d("LocationRepo", "✅ Seguimiento de ubicación detenido")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("LocationRepo", "❌ Error deteniendo seguimiento", e)
            Result.failure(e)
        }
    }
    
    override fun isLocationTrackingActive(): Flow<Boolean> = _isLocationTrackingActive
    
    override fun getLocationHistory(): Flow<List<LocationData>> = _locationHistory
    
    override fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000 // metros
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return (earthRadius * c).toFloat()
    }
    
    override suspend fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            android.util.Log.e("LocationRepo", "Error verificando servicios de ubicación", e)
            false
        }
    }
    
    override suspend fun requestLocationServices(): Result<Unit> {
        return try {
            android.util.Log.d("LocationRepo", "📱 Solicitando habilitar servicios de ubicación...")
            
            if (isLocationEnabled()) {
                android.util.Log.d("LocationRepo", "✅ Servicios de ubicación ya habilitados")
                return Result.success(Unit)
            }
            
            // TODO: Mostrar diálogo para habilitar ubicación
            android.util.Log.w("LocationRepo", "⚠️ Servicios de ubicación deshabilitados")
            Result.failure(Exception("Usuario debe habilitar servicios de ubicación"))
            
        } catch (e: Exception) {
            android.util.Log.e("LocationRepo", "❌ Error solicitando servicios", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza la ubicación actual y la agrega al historial
     */
    private fun updateLocation(location: LocationData) {
        _currentLocation.value = location
        
        val currentHistory = _locationHistory.value.toMutableList()
        currentHistory.add(location)
        
        // Mantener solo las últimas 100 ubicaciones
        if (currentHistory.size > 100) {
            currentHistory.removeAt(0)
        }
        
        _locationHistory.value = currentHistory
    }
    
    /**
     * Simula actualización de ubicación (para testing)
     */
    fun simulateLocationUpdate(lat: Double, lon: Double) {
        val location = LocationData(
            latitude = lat,
            longitude = lon,
            accuracy = 10f,
            timestamp = System.currentTimeMillis(),
            speed = 0f,
            bearing = 0f
        )
        updateLocation(location)
    }
}