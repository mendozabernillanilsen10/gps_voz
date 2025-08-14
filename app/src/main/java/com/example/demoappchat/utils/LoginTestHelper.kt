package com.example.demoappchat.utils

import android.util.Log
import com.example.demoappchat.data.repository.FirebaseRepository
import com.example.demoappchat.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class para testing y debugging de login
 */
object LoginTestHelper {
    
    fun testLoginFlow(repository: FirebaseRepository, email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LoginTest", "🧪 Iniciando test de login para: $email")
                
                val result = repository.loginUser(email, password)
                
                result.onSuccess { user ->
                    Log.d("LoginTest", "✅ Login exitoso: ${user.name} (${user.email})")
                    Log.d("LoginTest", "📋 Datos del usuario: $user")
                }.onFailure { exception ->
                    Log.e("LoginTest", "❌ Login falló: ${exception.message}")
                    exception.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e("LoginTest", "❌ Error en test de login", e)
            }
        }
    }
    
    fun testUserCreation(repository: FirebaseRepository, email: String, password: String, name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LoginTest", "🧪 Iniciando test de registro para: $email")
                
                val result = repository.registerUser(email, password, name)
                
                result.onSuccess { user ->
                    Log.d("LoginTest", "✅ Registro exitoso: ${user.name} (${user.email})")
                    Log.d("LoginTest", "📋 Datos del usuario: $user")
                }.onFailure { exception ->
                    Log.e("LoginTest", "❌ Registro falló: ${exception.message}")
                    exception.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e("LoginTest", "❌ Error en test de registro", e)
            }
        }
    }
    
    fun debugCurrentUser(repository: FirebaseRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = repository.currentUser.value
                Log.d("LoginTest", "👤 Usuario actual: $currentUser")
                
                if (currentUser != null) {
                    Log.d("LoginTest", "📋 Detalles del usuario:")
                    Log.d("LoginTest", "   - ID: ${currentUser.id}")
                    Log.d("LoginTest", "   - Nombre: ${currentUser.name}")
                    Log.d("LoginTest", "   - Email: ${currentUser.email}")
                    Log.d("LoginTest", "   - Activo: ${currentUser.isActive}")
                } else {
                    Log.d("LoginTest", "⚠️ No hay usuario actual")
                }
            } catch (e: Exception) {
                Log.e("LoginTest", "❌ Error obteniendo usuario actual", e)
            }
        }
    }
    
    fun testEmailExists(repository: FirebaseRepository, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LoginTest", "🧪 Verificando si existe el email: $email")
                
                val exists = repository.isEmailRegistered(email)
                
                if (exists) {
                    Log.d("LoginTest", "✅ El email $email ya está registrado")
                } else {
                    Log.d("LoginTest", "❌ El email $email no está registrado")
                }
            } catch (e: Exception) {
                Log.e("LoginTest", "❌ Error verificando email", e)
            }
        }
    }
    
    fun testRegisterOrLogin(repository: FirebaseRepository, email: String, password: String, name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LoginTest", "🧪 Probando registerOrLogin para: $email")
                
                val result = repository.registerOrLogin(email, password, name)
                
                result.onSuccess { user ->
                    Log.d("LoginTest", "✅ RegisterOrLogin exitoso: ${user.name} (${user.email})")
                }.onFailure { exception ->
                    Log.e("LoginTest", "❌ RegisterOrLogin falló: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e("LoginTest", "❌ Error en registerOrLogin", e)
            }
        }
    }
}
