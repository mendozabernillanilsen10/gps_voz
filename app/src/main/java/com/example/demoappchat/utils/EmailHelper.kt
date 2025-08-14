package com.example.demoappchat.utils

import android.util.Log
import com.example.demoappchat.data.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class para manejar problemas de email en registro/login
 */
object EmailHelper {
    
    /**
     * Verifica si un email existe y sugiere la acción apropiada
     */
    fun checkEmailAndSuggestAction(repository: FirebaseRepository, email: String, onResult: (EmailCheckResult) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EmailHelper", "🔍 Verificando email: $email")
                
                val exists = repository.isEmailRegistered(email)
                
                val result = if (exists) {
                    EmailCheckResult.Exists(
                        message = "El email $email ya está registrado. Intenta iniciar sesión.",
                        suggestion = "Usa el formulario de login en su lugar."
                    )
                } else {
                    EmailCheckResult.NotExists(
                        message = "El email $email está disponible para registro.",
                        suggestion = "Puedes proceder con el registro."
                    )
                }
                
                onResult(result)
                
            } catch (e: Exception) {
                Log.e("EmailHelper", "❌ Error verificando email", e)
                onResult(EmailCheckResult.Error(
                    message = "Error verificando email: ${e.message}",
                    suggestion = "Intenta de nuevo más tarde."
                ))
            }
        }
    }
    
    /**
     * Intenta registro o login inteligente
     */
    fun smartAuth(
        repository: FirebaseRepository, 
        email: String, 
        password: String, 
        name: String,
        onResult: (SmartAuthResult) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("EmailHelper", "🧠 Iniciando autenticación inteligente para: $email")
                
                val result = repository.registerOrLogin(email, password, name)
                
                result.onSuccess { user ->
                    Log.d("EmailHelper", "✅ Autenticación exitosa: ${user.name}")
                    onResult(SmartAuthResult.Success(user))
                }.onFailure { exception ->
                    Log.e("EmailHelper", "❌ Autenticación falló: ${exception.message}")
                    onResult(SmartAuthResult.Failure(exception.message ?: "Error desconocido"))
                }
            } catch (e: Exception) {
                Log.e("EmailHelper", "❌ Error en autenticación inteligente", e)
                onResult(SmartAuthResult.Failure(e.message ?: "Error desconocido"))
            }
        }
    }
    
    /**
     * Maneja el error específico de email ya registrado
     */
    fun handleEmailCollision(email: String): String {
        return "El email $email ya está registrado. " +
               "Si es tu cuenta, intenta iniciar sesión. " +
               "Si olvidaste tu contraseña, puedes restablecerla."
    }
}

/**
 * Resultado de verificación de email
 */
sealed class EmailCheckResult {
    data class Exists(
        val message: String,
        val suggestion: String
    ) : EmailCheckResult()
    
    data class NotExists(
        val message: String,
        val suggestion: String
    ) : EmailCheckResult()
    
    data class Error(
        val message: String,
        val suggestion: String
    ) : EmailCheckResult()
}

/**
 * Resultado de autenticación inteligente
 */
sealed class SmartAuthResult {
    data class Success(val user: com.example.demoappchat.data.model.User) : SmartAuthResult()
    data class Failure(val error: String) : SmartAuthResult()
}
