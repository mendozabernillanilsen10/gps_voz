package com.example.demoappchat.data.repository

import com.example.demoappchat.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandsRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) {
    
    suspend fun saveCommandActions(actions: Map<String, String>) {
        try {
            // Guardar en preferencias locales
            userPreferences.setCommandActions(actions)
            
            // Guardar en Firebase
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                val userCommandsRef = firebaseDatabase.reference
                    .child("users")
                    .child(userId)
                    .child("voiceCommands")
                
                // Convertir el mapa a un formato que Firebase pueda manejar
                val commandsData = actions.map { (command, action) ->
                    command to mapOf(
                        "action" to action,
                        "timestamp" to System.currentTimeMillis()
                    )
                }.toMap()
                
                userCommandsRef.setValue(commandsData).await()
            }
        } catch (e: Exception) {
            // Si Firebase falla, al menos tenemos las preferencias locales
            userPreferences.setCommandActions(actions)
        }
    }
    
    suspend fun loadCommandActions(): Map<String, String> {
        try {
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                val userCommandsRef = firebaseDatabase.reference
                    .child("users")
                    .child(userId)
                    .child("voiceCommands")
                
                val snapshot = userCommandsRef.get().await()
                if (snapshot.exists()) {
                    val actions = mutableMapOf<String, String>()
                    
                    for (child in snapshot.children) {
                        val command = child.key ?: continue
                        val actionData = child.child("action").getValue(String::class.java)
                        if (actionData != null) {
                            actions[command] = actionData
                        }
                    }
                    
                    // Guardar en preferencias locales también
                    userPreferences.setCommandActions(actions)
                    return actions
                }
            }
        } catch (e: Exception) {
            // Si Firebase falla, usar preferencias locales
        }
        
        // Fallback a preferencias locales
        return userPreferences.getCommandActions().first()
    }
    
    fun getCommandActionsFlow(): Flow<Map<String, String>> {
        return userPreferences.getCommandActions()
    }
    
    suspend fun addCommand(command: String, action: String) {
        val currentActions = loadCommandActions().toMutableMap()
        currentActions[command] = action
        saveCommandActions(currentActions)
    }
    
    suspend fun removeCommand(command: String) {
        val currentActions = loadCommandActions().toMutableMap()
        currentActions.remove(command)
        saveCommandActions(currentActions)
    }
    
    suspend fun updateCommandAction(command: String, action: String) {
        val currentActions = loadCommandActions().toMutableMap()
        currentActions[command] = action
        saveCommandActions(currentActions)
    }
} 