package com.example.demoappchat.domain.repository

/**
 * Repositorio para manejo de permisos del sistema
 */
interface PermissionRepository {
    
    /**
     * Verifica si los permisos están concedidos
     */
    suspend fun checkPermissions(permissions: List<String>): Result<Unit>
    
    /**
     * Solicita permisos al usuario
     */
    suspend fun requestPermissions(permissions: List<String>): Result<Map<String, Boolean>>
    
    /**
     * Verifica si la app está optimizada para batería
     */
    suspend fun isBatteryOptimized(): Boolean
    
    /**
     * Solicita deshabilitar optimización de batería
     */
    suspend fun requestDisableBatteryOptimization(): Result<Boolean>
    
    /**
     * Verifica permisos específicos para segundo plano
     */
    suspend fun checkBackgroundPermissions(): Result<Unit>
    
    /**
     * Verifica si puede mostrar sobre otras apps
     */
    suspend fun canDrawOverOtherApps(): Boolean
}