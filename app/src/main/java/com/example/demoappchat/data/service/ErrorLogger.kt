package com.example.demoappchat.data.service

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ErrorLogger {
    
    private val database = FirebaseDatabase.getInstance()
    private val errorsRef = database.getReference("app_errors")
    
    data class ErrorLog(
        val timestamp: Long = System.currentTimeMillis(),
        val deviceModel: String = Build.MODEL,
        val deviceBrand: String = Build.BRAND,
        val androidVersion: String = Build.VERSION.RELEASE,
        val appVersion: String = "1.0.0.2",
        val errorType: String = "",
        val errorMessage: String = "",
        val stackTrace: String = "",
        val userAction: String = "",
        val additionalData: Map<String, Any> = emptyMap()
    )
    
    fun logError(
        errorType: String,
        errorMessage: String,
        throwable: Throwable? = null,
        userAction: String = "",
        additionalData: Map<String, Any> = emptyMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val errorLog = ErrorLog(
                    errorType = errorType,
                    errorMessage = errorMessage,
                    stackTrace = throwable?.stackTraceToString() ?: "",
                    userAction = userAction,
                    additionalData = additionalData
                )
                
                // Log local para debugging inmediato
                Log.e("ErrorLogger", """
                    🚨 ERROR CAPTURADO:
                    Tipo: $errorType
                    Dispositivo: ${Build.BRAND} ${Build.MODEL}
                    Android: ${Build.VERSION.RELEASE}
                    Acción: $userAction
                    Mensaje: $errorMessage
                    Datos adicionales: $additionalData
                """.trimIndent())
                
                // Guardar en Firebase con timestamp único
                val errorId = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
                errorsRef.child(errorId).setValue(errorLog)
                    .addOnSuccessListener {
                        Log.d("ErrorLogger", "✅ Error guardado en Firebase: $errorId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ErrorLogger", "❌ Error guardando en Firebase", e)
                    }
                    
            } catch (e: Exception) {
                Log.e("ErrorLogger", "❌ Error en ErrorLogger", e)
            }
        }
    }
    
    fun logNavigationError(
        fromScreen: String,
        toScreen: String,
        chatId: String? = null,
        throwable: Throwable? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val data = mutableMapOf<String, Any>(
            "from_screen" to fromScreen,
            "to_screen" to toScreen
        )
        
        chatId?.let { data["chat_id"] = it }
        data.putAll(additionalData)
        
        logError(
            errorType = "NAVIGATION_ERROR",
            errorMessage = "Error navegando de $fromScreen a $toScreen",
            throwable = throwable,
            userAction = "navigate_to_chat",
            additionalData = data
        )
    }
    
    fun logChatCreationError(
        chatTitle: String,
        chatId: String? = null,
        step: String,
        throwable: Throwable? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val data = mutableMapOf<String, Any>(
            "chat_title" to chatTitle,
            "creation_step" to step
        )
        
        chatId?.let { data["chat_id"] = it }
        data.putAll(additionalData)
        
        logError(
            errorType = "CHAT_CREATION_ERROR",
            errorMessage = "Error en creación de chat: $step",
            throwable = throwable,
            userAction = "create_chat",
            additionalData = data
        )
    }
    
    fun logHonorSpecificIssue(
        issue: String,
        context: String,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        // Solo loggear si es dispositivo Honor
        if (Build.BRAND.contains("honor", ignoreCase = true) || 
            Build.BRAND.contains("HONOR", ignoreCase = true)) {
            
            val data = mutableMapOf<String, Any>(
                "honor_issue" to issue,
                "context" to context,
                "device_info" to "${Build.BRAND} ${Build.MODEL} ${Build.VERSION.RELEASE}"
            )
            data.putAll(additionalData)
            
            logError(
                errorType = "HONOR_SPECIFIC_ISSUE",
                errorMessage = "Honor device issue: $issue",
                userAction = "honor_specific_behavior",
                additionalData = data
            )
        }
    }
    
    companion object {
        fun isHonorDevice(): Boolean {
            return Build.BRAND.contains("honor", ignoreCase = true) || 
                   Build.BRAND.contains("HONOR", ignoreCase = true)
        }
        
        fun getDeviceInfo(): String {
            return "${Build.BRAND} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        }
    }
}