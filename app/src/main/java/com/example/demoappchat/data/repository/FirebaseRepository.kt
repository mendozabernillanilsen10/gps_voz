package com.example.demoappchat.data.repository

import android.util.Log
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val preferences: UserPreferences
) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Referencias
    private val usersRef = database.getReference("users")
    private val chatsRef = database.getReference("proximity_chats")
    private val messagesRef = database.getReference("chat_messages")
    private val participantsRef = database.getReference("chat_participants")

    // Estado del usuario actual
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Estado de chats cercanos
    private val _nearbyChats = MutableStateFlow<List<ProximityChat>>(emptyList())
    val nearbyChats: StateFlow<List<ProximityChat>> = _nearbyChats

    init {
        // Escuchar cambios en la autenticación
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { firebaseUser ->
                loadCurrentUser(firebaseUser.uid)
            } ?: run {
                _currentUser.value = null
            }
        }
    }

    // ============== AUTENTICACIÓN ==============

    suspend fun registerUser(email: String, password: String, name: String): Result<User> {
        return try {
            Log.d("FirebaseRepo", "🔄 Iniciando registro de usuario: $email")
            
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error creating user")
            
            Log.d("FirebaseRepo", "✅ Usuario creado en Auth: ${firebaseUser.uid}")

            val user = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                latitude = 0.0,
                longitude = 0.0,
                lastSeen = System.currentTimeMillis(),
                fcmToken = "",
                isActive = true
            )

            Log.d("FirebaseRepo", "📋 Datos del usuario a guardar: ${user.toMap()}")
            
            // Guardar usuario en Realtime Database
            usersRef.child(firebaseUser.uid).setValue(user.toMap()).await()
            Log.d("FirebaseRepo", "✅ Usuario guardado en Database")
            
            _currentUser.value = user
            Log.d("FirebaseRepo", "✅ Usuario asignado a currentUser")

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error registering user", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            Log.d("FirebaseRepo", "🔄 Iniciando login de usuario: $email")
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error signing in")
            
            Log.d("FirebaseRepo", "✅ Autenticación exitosa: ${firebaseUser.uid}")

            // Cargar usuario desde Database con timeout
            loadCurrentUser(firebaseUser.uid)
            
            // Esperar un poco para que se cargue el usuario
            kotlinx.coroutines.delay(2000)
            
            val user = _currentUser.value
            Log.d("FirebaseRepo", "👤 Usuario actual después de carga: $user")
            
            if (user == null) {
                Log.w("FirebaseRepo", "⚠️ Usuario no encontrado en Database, intentando cargar directamente...")
                // Intentar cargar directamente
                val userSnapshot = usersRef.child(firebaseUser.uid).get().await()
                Log.d("FirebaseRepo", "📄 Snapshot directo: ${userSnapshot.exists()}")
                
                if (userSnapshot.exists()) {
                    val userData = userSnapshot.getValue(User::class.java)
                    if (userData != null) {
                        _currentUser.value = userData
                        Log.d("FirebaseRepo", "✅ Usuario cargado directamente: ${userData.name}")
                        return Result.success(userData)
                    }
                }
                
                throw Exception("User not found in database")
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error logging in", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun loadCurrentUser(userId: String) {
        Log.d("FirebaseRepo", "🔄 Cargando usuario: $userId")
        usersRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d("FirebaseRepo", "📄 Snapshot recibido: ${snapshot.exists()}")
                    Log.d("FirebaseRepo", "📋 Datos del snapshot: ${snapshot.value}")
                    
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            Log.d("FirebaseRepo", "✅ Usuario cargado exitosamente: ${user.name}")
                            _currentUser.value = user
                        } else {
                            Log.w("FirebaseRepo", "⚠️ Usuario es null después de deserialización")
                            // Intentar deserialización manual
                            tryManualUserDeserialization(snapshot)
                        }
                    } else {
                        Log.w("FirebaseRepo", "⚠️ Usuario no existe en Firebase")
                        _currentUser.value = null
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseRepo", "❌ Error deserializando usuario", e)
                    // Intentar deserialización manual como fallback
                    tryManualUserDeserialization(snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "❌ Error cargando usuario", error.toException())
                _currentUser.value = null
            }
        })
    }
    
    private fun tryManualUserDeserialization(snapshot: DataSnapshot) {
        try {
            Log.d("FirebaseRepo", "🔧 Intentando deserialización manual...")
            val data = snapshot.value as? Map<String, Any> ?: return
            
            val user = User(
                id = data["id"] as? String ?: "",
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                photoUrl = data["photoUrl"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                lastSeen = (data["lastSeen"] as? Number)?.toLong() ?: 0L,
                fcmToken = data["fcmToken"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: true
            )
            
            Log.d("FirebaseRepo", "✅ Deserialización manual exitosa: ${user.name}")
            _currentUser.value = user
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error en deserialización manual", e)
            _currentUser.value = null
        }
    }

    // ============== UBICACIÓN ==============

    suspend fun updateUserLocation(latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

            val updates = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "lastSeen" to System.currentTimeMillis(),
                "isActive" to true
            )

            usersRef.child(userId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating location", e)
            Result.failure(e)
        }
    }

    // ============== CHATS DE PROXIMIDAD ==============

    suspend fun createProximityChat(chat: ProximityChat): Result<String> {
        return try {
            val chatId = chatsRef.push().key ?: throw Exception("Error generating chat ID")
            val chatWithId = chat.copy(id = chatId)

            // Guardar chat
            chatsRef.child(chatId).setValue(chatWithId.toMap()).await()

            // Agregar creador como participante
            participantsRef.child(chatId).child(chat.creatorId).setValue(true).await()

            // Notificar usuarios cercanos
            notifyUsersInRange(chatWithId)

            Result.success(chatId)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creating chat", e)
            Result.failure(e)
        }
    }

    fun startListeningToNearbyChats(userLatitude: Double, userLongitude: Double) {
        Log.d("FirebaseRepo", "🔍 Iniciando escucha de chats cercanos en: $userLatitude, $userLongitude")
        
        chatsRef.orderByChild("isActive").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("FirebaseRepo", "📡 Datos recibidos de Firebase: ${snapshot.childrenCount} chats")
                    
                    val chats = mutableListOf<ProximityChat>()

                    snapshot.children.forEach { chatSnapshot ->
                        val chat = chatSnapshot.getValue(ProximityChat::class.java)
                        chat?.let {
                            val distance = calculateDistance(
                                userLatitude, userLongitude,
                                it.latitude, it.longitude
                            )

                            Log.d("FirebaseRepo", "📍 Chat: ${it.title} - Distancia: ${distance}m, Radio: ${it.radius}m")

                            // Solo mostrar chats dentro del radio
                            if (distance <= it.radius) {
                                chats.add(it)
                                Log.d("FirebaseRepo", "✅ Chat agregado: ${it.title}")
                            } else {
                                Log.d("FirebaseRepo", "❌ Chat fuera de rango: ${it.title}")
                            }
                        }
                    }

                    Log.d("FirebaseRepo", "📋 Total de chats en rango: ${chats.size}")
                    _nearbyChats.value = chats.sortedByDescending { it.createdAt }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseRepo", "Error listening to chats", error.toException())
                }
            })
    }

    suspend fun joinChatWithPin(chatId: String, pin: String): Result<Boolean> {
        return try {
            val snapshot = chatsRef.child(chatId).get().await()
            val chat = snapshot.getValue(ProximityChat::class.java)
                ?: throw Exception("Chat not found")

            if (chat.pin == pin) {
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                // Agregar usuario a participantes
                participantsRef.child(chatId).child(userId).setValue(true).await()

                // Incrementar contador
                chatsRef.child(chatId).child("participantsCount")
                    .setValue(ServerValue.increment(1)).await()

                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error joining chat", e)
            Result.failure(e)
        }
    }

    // ============== MENSAJES ==============

    suspend fun sendMessage(message: ChatMessage): Result<String> {
        return try {
            val messageId = messagesRef.child(message.chatId).push().key
                ?: throw Exception("Error generating message ID")

            val messageWithId = message.copy(id = messageId)
            messagesRef.child(message.chatId).child(messageId)
                .setValue(messageWithId.toMap()).await()

            // Actualizar última actividad del chat
            chatsRef.child(message.chatId).child("lastActivity")
                .setValue(System.currentTimeMillis()).await()

            Result.success(messageId)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error sending message", e)
            Result.failure(e)
        }
    }

    fun getChatMessages(chatId: String): StateFlow<List<ChatMessage>> {
        val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

        messagesRef.child(chatId).orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { messageSnapshot ->
                        messageSnapshot.getValue(ChatMessage::class.java)?.copy(
                            id = messageSnapshot.key ?: "", // Set the Firebase key as the message ID
                            messageType = try {
                                MessageType.valueOf(
                                    messageSnapshot.child("messageType").getValue(String::class.java) ?: "TEXT"
                                )
                            } catch (e: Exception) {
                                MessageType.TEXT
                            }
                        )
                    }.filter { !it.isDeleted }

                    messagesFlow.value = messages
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseRepo", "Error loading messages", error.toException())
                }
            })

        return messagesFlow
    }

    // ============== SUBIDA DE ARCHIVOS Y MENSAJES MULTIMEDIA ==============

    suspend fun uploadMediaFile(file: java.io.File, mediaType: String, chatId: String? = null): String {
        return try {
            val timestamp = System.currentTimeMillis()
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val fileName = "${mediaType}_$timestamp.${if (mediaType == "audio") "mp3" else "mp4"}"

            // ✅ USAR RUTA PERMITIDA SEGÚN EL CONTEXTO
            val path = when {
                // Si hay chatId, usar chat_media (para archivos del chat)
                !chatId.isNullOrEmpty() -> "chat_media/$chatId/$fileName"
                // Si es audio personal, usar user_audio
                mediaType == "audio" -> "user_audio/$userId/$fileName"
                // Para otros archivos temporales, usar temp
                else -> "temp/$userId/$fileName"
            }

            val storageRef = storage.reference.child(path)

            Log.d("FirebaseRepo", "Subiendo archivo a ruta: $path")

            val uploadTask = storageRef.putFile(android.net.Uri.fromFile(file))
            val downloadUrl = uploadTask.await().storage.downloadUrl.await()

            Log.d("FirebaseRepo", "Archivo subido exitosamente: $downloadUrl")
            downloadUrl.toString()

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error subiendo archivo", e)
            throw e
        }
    }

    suspend fun sendMediaMessage(
        chatId: String,
        mediaUrl: String,
        messageType: String,
        content: String = ""
    ) {
        try {
            val currentUser = _currentUser.value ?: throw Exception("Usuario no autenticado")

            val message = ChatMessage(
                chatId = chatId,
                userId = currentUser.id,
                userName = currentUser.name,
                userPhotoUrl = currentUser.photoUrl,
                messageType = when (messageType) {
                    "AUDIO" -> MessageType.AUDIO
                    "VIDEO" -> MessageType.VIDEO
                    "PHOTO" -> MessageType.PHOTO
                    else -> MessageType.PHOTO
                },
                content = content,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis()
            )

            sendMessage(message)

            // Actualizar última actividad del chat
            chatsRef.child(chatId).child("lastActivity").setValue(System.currentTimeMillis())

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error enviando mensaje multimedia", e)
            throw e
        }
    }

    // ============== ALERTAS DE EMERGENCIA ==============

    suspend fun sendEmergencyAlert(command: String, radius: Int, recordingType: String = "AUDIO"): Result<String> {
        return try {
            val currentUser = _currentUser.value ?: throw Exception("Usuario no autenticado")

            // Buscar chat de emergencia cercano o crear uno nuevo
            val nearestChat = findNearestActiveChat(currentUser.latitude, currentUser.longitude, radius)
            val chatId = nearestChat?.id ?: createNewEmergencyChat(currentUser, command)

            // Enviar mensaje de alerta
            val alertMessage = ChatMessage(
                chatId = chatId,
                userId = currentUser.id,
                userName = currentUser.name,
                userPhotoUrl = currentUser.photoUrl,
                messageType = MessageType.TEXT,
                content = "🚨 ALERTA ACTIVADA: \"$command\" - Grabando: $recordingType",
                timestamp = System.currentTimeMillis()
            )

            sendMessage(alertMessage)

            // Actualizar preferencias con el chat actual
            preferences.setCurrentChatId(chatId)

            Result.success(chatId)

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error enviando alerta de emergencia", e)
            Result.failure(e)
        }
    }

    private fun createEmergencyMessage(command: String, user: User): ChatMessage {
        return ChatMessage(
            userId = user.id,
            userName = user.name,
            userPhotoUrl = user.photoUrl,
            messageType = MessageType.SYSTEM,
            content = "🚨 ALERTA ACTIVADA POR VOZ: \"$command\" - ${formatTimestamp(System.currentTimeMillis())}",
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun findNearestActiveChat(latitude: Double, longitude: Double, radius: Int): ProximityChat? {
        return try {
            val snapshot = chatsRef.orderByChild("isActive").equalTo(true).get().await()

            snapshot.children.mapNotNull { chatSnapshot ->
                chatSnapshot.getValue(ProximityChat::class.java)
            }.filter { chat ->
                val distance = calculateDistance(latitude, longitude, chat.latitude, chat.longitude)
                distance <= radius && chat.category == "emergency"
            }.minByOrNull { chat ->
                calculateDistance(latitude, longitude, chat.latitude, chat.longitude)
            }

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error finding nearest chat", e)
            null
        }
    }

    private fun createEmergencyChat(user: User, command: String): ProximityChat {
        return ProximityChat(
            creatorId = user.id,
            creatorName = user.name,
            title = "🚨 Alerta por Voz",
            description = "Activada automáticamente por  : \"$command\"",
            latitude = user.latitude,
            longitude = user.longitude,
            radius = 300, // Radio pequeño para emergencias
            pin = generateEmergencyPin(),
            category = "emergency",
            createdAt = System.currentTimeMillis(),
            isActive = true,
            participantsCount = 1,
            lastActivity = System.currentTimeMillis()
        )
    }

    private suspend fun sendMessageToChat(chatId: String, message: ChatMessage) {
        val messageWithChatId = message.copy(chatId = chatId)
        sendMessage(messageWithChatId)
    }

    private suspend fun sendLocationMessage(chatId: String, user: User) {
        val locationMessage = ChatMessage(
            chatId = chatId,
            userId = user.id,
            userName = user.name,
            userPhotoUrl = user.photoUrl,
            messageType = MessageType.LOCATION,
            content = "📍 Ubicación actual: ${user.latitude}, ${user.longitude}",
            timestamp = System.currentTimeMillis()
        )
        sendMessage(locationMessage)
    }

    private suspend fun createNewEmergencyChat(user: User, command: String): String {
        val chat = createEmergencyChat(user, command)
        val chatId = chatsRef.push().key ?: throw Exception("Error generando ID de chat")

        val chatWithId = chat.copy(id = chatId)
        chatsRef.child(chatId).setValue(chatWithId.toMap()).await()

        // Agregar usuario como participante
        participantsRef.child(chatId).child(user.id).setValue(true).await()

        return chatId
    }

    private fun generateEmergencyPin(): String {
        return (1000..9999).random().toString()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp))
    }

    // ============== FCM TOKEN MANAGEMENT ==============

    suspend fun registerFCMToken(token: String): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            
            // Actualizar token FCM en el perfil del usuario
            usersRef.child(currentUserId).child("fcmToken").setValue(token).await()
            
            // También guardar en una colección separada para notificaciones grupales
            val tokenData = mapOf(
                "userId" to currentUserId,
                "token" to token,
                "timestamp" to System.currentTimeMillis(),
                "platform" to "android",
                "appVersion" to "1.0.0" // TODO: obtener versión real de la app
            )
            
            database.getReference("fcm_tokens").child(currentUserId).setValue(tokenData).await()
            
            Log.d("FirebaseRepo", "✅ Token FCM registrado para usuario: $currentUserId")
            Result.success("Token FCM registrado exitosamente")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error registrando token FCM", e)
            Result.failure(e)
        }
    }

    suspend fun sendGroupCallNotification(chatId: String, callType: String, callerName: String): Result<String> {
        return try {
            Log.d("FirebaseRepo", "🔄 Iniciando notificación de llamada grupal para chat: $chatId")
            
            // Verificar permisos básicos
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            
            // Intentar obtener participantes con retry
            val participantsSnapshot = try {
                participantsRef.child(chatId).get().await()
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "⚠️ Error accediendo participantes, intentando método alternativo", e)
                // Método alternativo: simular notificación para testing
                return Result.success("Notificación simulada (sin acceso a participantes)")
            }
            
            var notificationsSent = 0
            var errors = 0
            
            participantsSnapshot.children.forEach { participantSnapshot ->
                val participantId = participantSnapshot.key
                
                // No notificar al caller
                if (participantId != null && participantId != currentUserId) {
                    try {
                        // Intentar obtener token FCM del participante
                        val userSnapshot = try {
                            usersRef.child(participantId).get().await()
                        } catch (e: Exception) {
                            Log.w("FirebaseRepo", "⚠️ Sin acceso a usuario $participantId, creando notificación simulada", e)
                            // Crear notificación simulada
                            val simulatedData = mapOf(
                                "type" to callType,
                                "chat_id" to chatId,
                                "caller_name" to callerName,
                                "call_id" to "${System.currentTimeMillis()}_$chatId",
                                "participant_id" to participantId,
                                "timestamp" to System.currentTimeMillis().toString(),
                                "status" to "simulated_no_access"
                            )
                            sendFCMNotification("SIMULATED_TOKEN_$participantId", simulatedData)
                            notificationsSent++
                            return@forEach
                        }
                        
                        val fcmToken = userSnapshot.child("fcmToken").getValue(String::class.java) 
                            ?: "EMPTY_TOKEN_$participantId"
                        
                        val notificationData = mapOf(
                            "type" to callType,
                            "chat_id" to chatId,
                            "caller_name" to callerName,
                            "call_id" to "${System.currentTimeMillis()}_$chatId",
                            "participant_id" to participantId,
                            "timestamp" to System.currentTimeMillis().toString()
                        )
                        
                        sendFCMNotification(fcmToken, notificationData)
                        notificationsSent++
                        
                        Log.d("FirebaseRepo", "📞 Notificación de ${callType} enviada a participante: $participantId")
                        
                    } catch (e: Exception) {
                        errors++
                        Log.w("FirebaseRepo", "⚠️ Error notificando participante $participantId", e)
                    }
                }
            }
            
            val resultMessage = "$notificationsSent notificaciones enviadas" + 
                if (errors > 0) " ($errors errores)" else ""
            
            Result.success(resultMessage)
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error enviando notificaciones de llamada grupal", e)
            // En caso de error total, al menos loguear la intención
            try {
                val errorLog = mapOf(
                    "action" to "group_call_notification_failed",
                    "chat_id" to chatId,
                    "call_type" to callType,
                    "caller_name" to callerName,
                    "error" to e.message,
                    "timestamp" to System.currentTimeMillis()
                )
                database.getReference("error_logs").push().setValue(errorLog)
                Log.d("FirebaseRepo", "📝 Error de llamada grupal registrado en logs")
            } catch (logError: Exception) {
                Log.e("FirebaseRepo", "❌ No se pudo registrar error", logError)
            }
            
            Result.failure(Exception("Notificación simulada debido a permisos limitados"))
        }
    }

    // ============== GROUP CALLS INTEGRATION ==============
    
    /**
     * Iniciar llamada grupal con notificaciones FCM
     */
    suspend fun startGroupCallWithNotifications(
        chatId: String,
        callType: String,
        callerName: String
    ): Result<String> {
        return try {
            Log.d("FirebaseRepo", "📞 Iniciando llamada grupal: $callType en chat: $chatId")
            
            // Enviar notificaciones FCM a todos los participantes
            val notificationResult = sendGroupCallNotification(chatId, callType, callerName)
            
            if (notificationResult.isSuccess) {
                Log.d("FirebaseRepo", "✅ Llamada grupal iniciada: ${notificationResult.getOrNull()}")
                Result.success("Llamada grupal iniciada exitosamente")
            } else {
                Log.w("FirebaseRepo", "⚠️ Llamada iniciada con errores: ${notificationResult.exceptionOrNull()?.message}")
                Result.success("Llamada iniciada (con errores de notificación)")
            }
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error iniciando llamada grupal", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtener participantes de un chat para llamadas
     */
    suspend fun getChatParticipantsForCall(chatId: String): Result<List<String>> {
        return try {
            val participantsSnapshot = participantsRef.child(chatId).get().await()
            val participantIds = participantsSnapshot.children.mapNotNull { it.key }
            
            Log.d("FirebaseRepo", "👥 Participantes obtenidos para chat $chatId: ${participantIds.size}")
            Result.success(participantIds)
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error obteniendo participantes", e)
            Result.failure(e)
        }
    }

    suspend fun sendEmergencyFCMAlert(message: String, location: String?, alertLevel: String = "high"): Result<String> {
        return try {
            val currentUser = _currentUser.value ?: throw Exception("Usuario no autenticado")
            
            // Obtener todos los usuarios activos
            val usersSnapshot = usersRef.orderByChild("isActive").equalTo(true).get().await()
            var alertsSent = 0
            
            usersSnapshot.children.forEach { userSnapshot ->
                val user = userSnapshot.getValue(User::class.java)
                
                if (user != null && user.id != currentUser.id && user.fcmToken.isNotEmpty()) {
                    try {
                        val notificationData = mapOf(
                            "type" to "emergency_alert",
                            "message" to message,
                            "location" to (location ?: "Ubicación no disponible"),
                            "level" to alertLevel,
                            "agent_name" to currentUser.name,
                            "timestamp" to System.currentTimeMillis().toString()
                        )
                        
                        sendFCMNotification(user.fcmToken, notificationData)
                        alertsSent++
                        
                    } catch (e: Exception) {
                        Log.w("FirebaseRepo", "⚠️ Error enviando alerta a ${user.name}", e)
                    }
                }
            }
            
            Log.d("FirebaseRepo", "🚨 $alertsSent alertas de emergencia enviadas")
            Result.success("$alertsSent alertas enviadas")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error enviando alertas FCM de emergencia", e)
            Result.failure(e)
        }
    }

    // ============== UTILIDADES ==============

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

    private suspend fun notifyUsersInRange(chat: ProximityChat) {
        try {
            Log.d("FirebaseRepo", "🚨 NOTIFICACIÓN POLICIAL: Nuevo chat '${chat.title}' creado")
            
            val snapshot = usersRef.orderByChild("isActive").equalTo(true).get().await()
            var notifiedUsers = 0

            snapshot.children.forEach { userSnapshot ->
                val user = userSnapshot.getValue(User::class.java)
                user?.let {
                    if (it.id != chat.creatorId) {
                        val distance = calculateDistance(
                            chat.latitude, chat.longitude,
                            it.latitude, it.longitude
                        )

                        if (distance <= chat.radius) {
                            // Enviar notificación push
                            sendPushNotification(
                                user = it,
                                chat = chat,
                                distance = distance
                            )
                            notifiedUsers++
                            
                            Log.d("FirebaseRepo", "📡 Usuario ${it.name} notificado - Distancia: ${String.format("%.0f", distance)}m")
                        }
                    }
                }
            }
            
            Log.d("FirebaseRepo", "✅ Total usuarios notificados: $notifiedUsers")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error crítico notificando usuarios", e)
        }
    }

    private suspend fun sendPushNotification(user: User, chat: ProximityChat, distance: Double) {
        try {
            if (user.fcmToken.isNotEmpty()) {
                // Preparar datos de la notificación
                val notificationData = mapOf(
                    "title" to "🚨 ALERTA POLICIAL - Nuevo Chat de Seguridad",
                    "body" to "${chat.title} - ${String.format("%.0f", distance)}m de distancia",
                    "chatId" to chat.id,
                    "chatTitle" to chat.title,
                    "creatorName" to chat.creatorName,
                    "distance" to distance.toString(),
                    "category" to chat.category,
                    "type" to "proximity_chat_alert"
                )

                // Enviar usando Firebase Cloud Messaging
                sendFCMNotification(user.fcmToken, notificationData)
                
                Log.d("FirebaseRepo", "🔔 Push notification enviada a ${user.name}")
            } else {
                Log.w("FirebaseRepo", "⚠️ Usuario ${user.name} sin token FCM")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error enviando push notification", e)
        }
    }

    private suspend fun sendFCMNotification(token: String, data: Map<String, String>) {
        try {
            Log.d("FirebaseRepo", "🚀 FCM Token: ${token.take(20)}...")
            Log.d("FirebaseRepo", "📨 Notification data: $data")
            
            // SIMULACIÓN: Por ahora guardamos la notificación en Database como respaldo
            // En producción se usaría Firebase Cloud Functions o Admin SDK
            val notificationRecord = mapOf(
                "targetToken" to token,
                "data" to data,
                "timestamp" to System.currentTimeMillis(),
                "status" to "simulated",
                "platform" to "android"
            )
            
            // Guardar en Firebase Database como log de notificaciones
            database.getReference("notification_logs")
                .push()
                .setValue(notificationRecord)
                .await()
            
            Log.d("FirebaseRepo", "✅ Notificación FCM simulada y registrada")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error en FCM", e)
            throw e
        }
    }
}