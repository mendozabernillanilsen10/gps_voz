package com.example.demoappchat.data.repository

import android.util.Log
import com.example.demoappchat.data.VoiceServicePreferences
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
    private val preferences: VoiceServicePreferences
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
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error creating user")

            val user = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                lastSeen = System.currentTimeMillis(),
                isActive = true
            )

            // Guardar usuario en Realtime Database
            usersRef.child(firebaseUser.uid).setValue(user.toMap()).await()
            _currentUser.value = user

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error registering user", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error signing in")

            loadCurrentUser(firebaseUser.uid)
            val user = _currentUser.value ?: throw Exception("User not found")

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error logging in", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun loadCurrentUser(userId: String) {
        usersRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                _currentUser.value = user
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "Error loading user", error.toException())
            }
        })
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
        chatsRef.orderByChild("isActive").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chats = mutableListOf<ProximityChat>()

                    snapshot.children.forEach { chatSnapshot ->
                        val chat = chatSnapshot.getValue(ProximityChat::class.java)
                        chat?.let {
                            val distance = calculateDistance(
                                userLatitude, userLongitude,
                                it.latitude, it.longitude
                            )

                            // Solo mostrar chats dentro del radio
                            if (distance <= it.radius) {
                                chats.add(it)
                            }
                        }
                    }

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
                messageType = if (messageType == "AUDIO") MessageType.AUDIO else MessageType.VIDEO,
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
            preferences.currentChatId = chatId

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
            description = "Activada automáticamente por comando: \"$command\"",
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
            val snapshot = usersRef.orderByChild("isActive").equalTo(true).get().await()

            snapshot.children.forEach { userSnapshot ->
                val user = userSnapshot.getValue(User::class.java)
                user?.let {
                    if (it.id != chat.creatorId) {
                        val distance = calculateDistance(
                            chat.latitude, chat.longitude,
                            it.latitude, it.longitude
                        )

                        if (distance <= chat.radius) {
                            Log.d("FirebaseRepo", "Notifying user ${it.name} about chat ${chat.title}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error notifying users", e)
        }
    }
}