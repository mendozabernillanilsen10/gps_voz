package com.example.demoappchat.utils

import android.util.Log
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.demoappchat.data.service.ErrorLogger

object NavigationHelper {
    
    data class NavigationAttempt(
        val chatId: String,
        val timestamp: Long = System.currentTimeMillis(),
        val attemptCount: Int = 0
    )
    
    private val pendingNavigations = mutableMapOf<String, NavigationAttempt>()
    private const val MAX_ATTEMPTS = 3
    private const val ATTEMPT_DELAY = 1000L // 1 segundo entre intentos
    
    /**
     * Navega a un chat de forma robusta con reintentos automáticos
     */
    fun navigateToChat(
        navController: NavController,
        chatId: String,
        errorLogger: ErrorLogger? = null,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("NavigationHelper", "🚀 Iniciando navegación robusta a chat: $chatId")
                
                // Log para Honor devices
             //   if (ErrorLogger.isHonorDevice()) {
                    errorLogger?.logHonorSpecificIssue(
                        issue = "robust_navigation_start",
                        context = "NavigationHelper.navigateToChat",
                        additionalData = mapOf(
                            "chat_id" to chatId,
                            "device_info" to ErrorLogger.getDeviceInfo()
                        )
                    )
               // }
                
                val success = performNavigationWithRetries(
                    navController = navController,
                    chatId = chatId,
                    errorLogger = errorLogger
                )
                
                if (success) {
                    Log.d("NavigationHelper", "✅ Navegación exitosa a chat: $chatId")
                    onSuccess?.invoke()
                } else {
                    Log.e("NavigationHelper", "❌ Navegación falló después de $MAX_ATTEMPTS intentos: $chatId")
                    val error = Exception("Navegación falló después de $MAX_ATTEMPTS intentos")
                    onFailure?.invoke(error)
                }
                
            } catch (e: Exception) {
                Log.e("NavigationHelper", "❌ Error en navegación robusta: $chatId", e)
                onFailure?.invoke(e)
            } finally {
                // Limpiar navegación pendiente
                pendingNavigations.remove(chatId)
            }
        }
    }
    
    private suspend fun performNavigationWithRetries(
        navController: NavController,
        chatId: String,
        errorLogger: ErrorLogger?
    ): Boolean {
        var currentAttempt = pendingNavigations[chatId] ?: NavigationAttempt(chatId)
        
        for (attemptIndex in 0 until MAX_ATTEMPTS) {
            try {
                Log.d("NavigationHelper", "🔄 Intento ${attemptIndex + 1}/$MAX_ATTEMPTS para chat: $chatId")
                
                // Delay progresivo: 300ms, 600ms, 1000ms
                val delay = when {
                    ErrorLogger.isHonorDevice() -> (300L * (attemptIndex + 1))
                    else -> (200L * (attemptIndex + 1))
                }
                
                delay(delay)
                
                // Verificar que navController sigue siendo válido
                withContext(Dispatchers.Main) {
                    val currentDestination = navController.currentDestination?.route
                    Log.d("NavigationHelper", "📍 Destino actual: $currentDestination")
                    
                    // Intentar navegación
                    navController.navigate("chat/$chatId") {
                        launchSingleTop = true
                        restoreState = true
                    }
                    
                    Log.d("NavigationHelper", "🎯 Navegación ejecutada para intento ${attemptIndex + 1}")
                }
                
                // Verificar si la navegación fue exitosa
                delay(500) // Dar tiempo para que la navegación se complete
                
                val navigationSuccess = withContext(Dispatchers.Main) {
                    val newDestination = navController.currentDestination?.route
                    if (newDestination?.contains("chat") == true) {
                        Log.d("NavigationHelper", "✅ Navegación confirmada exitosa en intento ${attemptIndex + 1}")
                        
                       // if (ErrorLogger.isHonorDevice()) {
                            errorLogger?.logHonorSpecificIssue(
                                issue = "robust_navigation_success",
                                context = "NavigationHelper.performNavigationWithRetries",
                                additionalData = mapOf(
                                    "chat_id" to chatId,
                                    "attempt" to (attemptIndex + 1),
                                    "delay_used" to delay,
                                    "destination" to (newDestination ?: "unknown")
                                )
                            )
                       // }
                        
                        true // Navegación exitosa
                    } else {
                        Log.w("NavigationHelper", "⚠️ Navegación no confirmada en intento ${attemptIndex + 1}. Destino: $newDestination")
                        
                       // if (ErrorLogger.isHonorDevice()) {
                            errorLogger?.logHonorSpecificIssue(
                                issue = "navigation_not_confirmed",
                                context = "NavigationHelper.performNavigationWithRetries",
                                additionalData = mapOf(
                                    "chat_id" to chatId,
                                    "attempt" to (attemptIndex + 1),
                                    "expected" to "chat/$chatId",
                                    "actual" to (newDestination ?: "null")
                                )
                            )
                       // }
                        
                        false // Navegación no confirmada
                    }
                }
                
                if (navigationSuccess) {
                    return true
                } else {
                    throw Exception("Navegación no confirmada")
                }
                
            } catch (e: Exception) {
                Log.w("NavigationHelper", "⚠️ Intento ${attemptIndex + 1} falló para chat $chatId: ${e.message}")
                
                currentAttempt = currentAttempt.copy(
                    attemptCount = attemptIndex + 1,
                    timestamp = System.currentTimeMillis()
                )
                pendingNavigations[chatId] = currentAttempt
                
                if (attemptIndex < MAX_ATTEMPTS - 1) {
                    // Esperar antes del siguiente intento
                    delay(ATTEMPT_DELAY)
                } else {
                    // Último intento falló
                    errorLogger?.logNavigationError(
                        fromScreen = "main",
                        toScreen = "chat",
                        chatId = chatId,
                        throwable = e,
                        additionalData = mapOf(
                            "navigation_helper" to "robust_navigation",
                            "total_attempts" to MAX_ATTEMPTS,
                            "final_attempt" to true,
                            "device_info" to ErrorLogger.getDeviceInfo()
                        )
                    )
                }
            }
        }
        
        return false // Todos los intentos fallaron
    }
    
    /**
     * Verifica si hay navegaciones pendientes para un chat
     */
    fun hasPendingNavigation(chatId: String): Boolean {
        return pendingNavigations.containsKey(chatId)
    }
    
    /**
     * Cancela una navegación pendiente
     */
    fun cancelPendingNavigation(chatId: String) {
        pendingNavigations.remove(chatId)
        Log.d("NavigationHelper", "🚫 Navegación cancelada para chat: $chatId")
    }
    
    /**
     * Limpia todas las navegaciones pendientes
     */
    fun clearAllPendingNavigations() {
        val count = pendingNavigations.size
        pendingNavigations.clear()
        Log.d("NavigationHelper", "🧹 Limpiadas $count navegaciones pendientes")
    }
    
    /**
     * Obtiene información de navegaciones pendientes para debugging
     */
    fun getPendingNavigationsInfo(): Map<String, NavigationAttempt> {
        return pendingNavigations.toMap()
    }
}