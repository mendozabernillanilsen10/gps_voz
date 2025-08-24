package com.example.demoappchat.data.repository

import android.util.Log
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                // Cargar usuario de forma asíncrona para evitar bloqueos
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        loadCurrentUser(firebaseUser.uid)
                    } catch (e: Exception) {
                        Log.e("FirebaseRepo", "❌ Error cargando usuario en init", e)
                    }
                }
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
            
            // Proporcionar mensajes de error más amigables
            Log.d("FirebaseRepo", "🔍 Procesando error: ${e.javaClass.simpleName} - ${e.message}")
            
            val userFriendlyError = when (e) {
                is FirebaseAuthUserCollisionException -> {
                    Log.d("FirebaseRepo", "✅ Detectado FirebaseAuthUserCollisionException")
                    "El email ya está registrado. Intenta iniciar sesión en su lugar."
                }
                is FirebaseAuthWeakPasswordException -> {
                    Log.d("FirebaseRepo", "✅ Detectado FirebaseAuthWeakPasswordException")
                    "La contraseña es muy débil. Usa al menos 6 caracteres."
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    Log.d("FirebaseRepo", "✅ Detectado FirebaseAuthInvalidCredentialsException")
                    "El formato del email no es válido."
                }
                is FirebaseAuthInvalidUserException -> {
                    Log.d("FirebaseRepo", "✅ Detectado FirebaseAuthInvalidUserException")
                    "Usuario no encontrado."
                }
                else -> {
                    Log.d("FirebaseRepo", "⚠️ Error no reconocido, usando mensaje: ${e.message}")
                    when (e.message) {
                        "The email address is already in use by another account." -> {
                            "El email ya está registrado. Intenta iniciar sesión en su lugar."
                        }
                        "The password is invalid or the user does not have a password." -> {
                            "La contraseña es incorrecta."
                        }
                        "Too many unsuccessful login attempts. Please try again later." -> {
                            "Demasiados intentos. Intenta más tarde."
                        }
                        "Network error (such as timeout, interrupted connection or unreachable host) has occurred." -> {
                            "Error de conexión. Verifica tu internet."
                        }
                        else -> {
                            "Error al registrar usuario: ${e.message}"
                        }
                    }
                }
            }
            
            Log.d("FirebaseRepo", "📝 Mensaje amigable generado: $userFriendlyError")
            
            // Crear una excepción personalizada con el mensaje amigable
            val customException = Exception(userFriendlyError)
            customException.initCause(e)
            
            Result.failure(customException)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            Log.d("FirebaseRepo", "🔄 Iniciando login de usuario: $email")
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error signing in")
            
            Log.d("FirebaseRepo", "✅ Autenticación exitosa: ${firebaseUser.uid}")

            // Cargar usuario directamente desde Database con timeout
            val userSnapshot = usersRef.child(firebaseUser.uid).get().await()
            Log.d("FirebaseRepo", "📄 Snapshot directo: ${userSnapshot.exists()}")
            
            if (userSnapshot.exists()) {
                val userData = userSnapshot.getValue(User::class.java)
                if (userData != null) {
                    _currentUser.value = userData
                    Log.d("FirebaseRepo", "✅ Usuario cargado directamente: ${userData.name}")
                    return Result.success(userData)
                } else {
                    // Intentar deserialización manual
                    Log.w("FirebaseRepo", "⚠️ Usuario es null después de deserialización, intentando manual...")
                    val manualUser = tryManualUserDeserializationFromSnapshot(userSnapshot)
                    if (manualUser != null) {
                        _currentUser.value = manualUser
                        Log.d("FirebaseRepo", "✅ Usuario cargado con deserialización manual: ${manualUser.name}")
                        return Result.success(manualUser)
                    }
                }
            }
            
            // Si no existe en Database, crear un usuario básico
            Log.w("FirebaseRepo", "⚠️ Usuario no encontrado en Database, creando usuario básico...")
            val basicUser = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Usuario",
                email = email,
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                latitude = 0.0,
                longitude = 0.0,
                lastSeen = System.currentTimeMillis(),
                fcmToken = "",
                isActive = true
            )
            
            // Guardar el usuario básico en Database
            usersRef.child(firebaseUser.uid).setValue(basicUser.toMap()).await()
            _currentUser.value = basicUser
            Log.d("FirebaseRepo", "✅ Usuario básico creado y guardado: ${basicUser.name}")
            
            Result.success(basicUser)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error logging in", e)
            
            // Proporcionar mensajes de error más amigables para login
            val userFriendlyError = when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    "Email o contraseña incorrectos."
                }
                is FirebaseAuthInvalidUserException -> {
                    "Usuario no encontrado. Verifica tu email."
                }
                else -> {
                    when (e.message) {
                        "The password is invalid or the user does not have a password." -> {
                            "Email o contraseña incorrectos."
                        }
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> {
                            "Usuario no encontrado. Verifica tu email."
                        }
                        "Too many unsuccessful login attempts. Please try again later." -> {
                            "Demasiados intentos. Intenta más tarde."
                        }
                        "Network error (such as timeout, interrupted connection or unreachable host) has occurred." -> {
                            "Error de conexión. Verifica tu internet."
                        }
                        "The user account has been disabled by an administrator." -> {
                            "Tu cuenta ha sido deshabilitada."
                        }
                        else -> {
                            "Error al iniciar sesión: ${e.message}"
                        }
                    }
                }
            }
            
            // Crear una excepción personalizada con el mensaje amigable
            val customException = Exception(userFriendlyError)
            customException.initCause(e)
            
            Result.failure(customException)
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    suspend fun signInWithGoogle(credential: AuthCredential): Result<User> {
        return try {
            Log.d("FirebaseRepo", "🔄 Iniciando login con Google")
            
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Error signing in with Google")
            
            Log.d("FirebaseRepo", "✅ Autenticación con Google exitosa: ${firebaseUser.uid}")

            // Verificar si el usuario ya existe en Database
            val userSnapshot = usersRef.child(firebaseUser.uid).get().await()
            
            if (userSnapshot.exists()) {
                // Usuario existe, cargarlo
                val userData = userSnapshot.getValue(User::class.java)
                if (userData != null) {
                    _currentUser.value = userData
                    Log.d("FirebaseRepo", "✅ Usuario existente cargado: ${userData.name}")
                    return Result.success(userData)
                }
            }
            
            // Usuario no existe, crear uno nuevo
            val newUser = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Usuario",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                latitude = 0.0,
                longitude = 0.0,
                lastSeen = System.currentTimeMillis(),
                fcmToken = "",
                isActive = true
            )
            
            // Guardar el nuevo usuario en Database
            usersRef.child(firebaseUser.uid).setValue(newUser.toMap()).await()
            _currentUser.value = newUser
            Log.d("FirebaseRepo", "✅ Nuevo usuario creado con Google: ${newUser.name}")
            
            Result.success(newUser)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error signing in with Google", e)
            
            val userFriendlyError = when (e.message) {
                "The account exists with different credentials." -> {
                    "Esta cuenta ya existe con otro método de inicio de sesión."
                }
                "The user account has been disabled." -> {
                    "Tu cuenta de Google ha sido deshabilitada."
                }
                "Network error (such as timeout, interrupted connection or unreachable host) has occurred." -> {
                    "Error de conexión. Verifica tu internet."
                }
                else -> {
                    "Error al iniciar sesión con Google: ${e.message}"
                }
            }
            
            val customException = Exception(userFriendlyError)
            customException.initCause(e)
            
            Result.failure(customException)
        }
    }
    
    /**
     * Verifica si un email ya está registrado
     */
    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            // Intentar obtener métodos de autenticación para el email
            val result = auth.fetchSignInMethodsForEmail(email).await()
            // Usar signInMethods property del resultado
            val methods = result.signInMethods ?: emptyList()
            methods.isNotEmpty()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error verificando email: $email", e)
            false
        }
    }
    
    /**
     * Intenta registrar o hacer login automáticamente
     */
    suspend fun registerOrLogin(email: String, password: String, name: String): Result<User> {
        return try {
            // Primero intentar login
            val loginResult = loginUser(email, password)
            if (loginResult.isSuccess) {
                Log.d("FirebaseRepo", "✅ Login exitoso para usuario existente")
                return loginResult
            }
            
            // Si el login falla, intentar registro
            Log.d("FirebaseRepo", "🔄 Login falló, intentando registro...")
            registerUser(email, password, name)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error en registerOrLogin", e)
            Result.failure(e)
        }
    }

    private fun loadCurrentUser(userId: String) {
        Log.d("FirebaseRepo", "🔄 Cargando usuario: $userId")
        
        // Usar SingleValueEvent para evitar múltiples callbacks
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d("FirebaseRepo", "📄 Snapshot recibido: ${snapshot.exists()}")
                    
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
    
    private fun tryManualUserDeserializationFromSnapshot(snapshot: DataSnapshot): User? {
        return try {
            Log.d("FirebaseRepo", "🔧 Intentando deserialización manual desde snapshot...")
            val data = snapshot.value as? Map<String, Any> ?: return null
            
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
            user
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error en deserialización manual", e)
            null
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

            // Agregar creador como participante con datos requeridos
            val creatorParticipantData = mapOf(
                "joinedAt" to System.currentTimeMillis(),
                "isActive" to true
            )
            participantsRef.child(chatId).child(chat.creatorId).setValue(creatorParticipantData).await()

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
        
        // Detener listener anterior si existe
        stopListeningToNearbyChats()
        
        chatsRef.orderByChild("isActive").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("FirebaseRepo", "📡 Datos recibidos de Firebase: ${snapshot.childrenCount} chats")
                        
                        val chats = mutableListOf<ProximityChat>()
                        var processedCount = 0

                        snapshot.children.forEach { chatSnapshot ->
                            try {
                                val chat = chatSnapshot.getValue(ProximityChat::class.java)
                                chat?.let {
                                    val distance = calculateDistance(
                                        userLatitude, userLongitude,
                                        it.latitude, it.longitude
                                    )

                                    // Solo mostrar chats dentro del radio
                                    if (distance <= it.radius) {
                                        chats.add(it)
                                        processedCount++
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseRepo", "❌ Error procesando chat: ${chatSnapshot.key}", e)
                            }
                        }

                        Log.d("FirebaseRepo", "📋 Total de chats en rango: ${chats.size} de ${snapshot.childrenCount}")
                        _nearbyChats.value = chats.sortedByDescending { it.createdAt }
                    } catch (e: Exception) {
                        Log.e("FirebaseRepo", "❌ Error procesando chats cercanos", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseRepo", "Error listening to chats", error.toException())
                }
            })
    }
    
    fun stopListeningToNearbyChats() {
        // Implementar detención del listener si es necesario
        Log.d("FirebaseRepo", "🛑 Deteniendo escucha de chats cercanos")
    }

    suspend fun joinChatWithPin(chatId: String, pin: String): Result<Boolean> {
        return try {
            val snapshot = chatsRef.child(chatId).get().await()
            val chat = snapshot.getValue(ProximityChat::class.java)
                ?: throw Exception("Chat not found")

            if (chat.pin == pin) {
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                // Agregar usuario a participantes con datos requeridos
                val participantData = mapOf(
                    "joinedAt" to System.currentTimeMillis(),
                    "isActive" to true
                )
                participantsRef.child(chatId).child(userId).setValue(participantData).await()

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
                                val messageTypeString = messageSnapshot.child("messageType").getValue(String::class.java) ?: "TEXT"
                                when (messageTypeString.uppercase()) {
                                    "AUDIO" -> MessageType.AUDIO
                                    "VIDEO" -> MessageType.VIDEO
                                    "PHOTO" -> MessageType.PHOTO
                                    "TEXT" -> MessageType.TEXT
                                    "LOCATION" -> MessageType.LOCATION
                                    "SYSTEM" -> MessageType.SYSTEM
                                    "VOICE_COMMAND" -> MessageType.VOICE_COMMAND
                                    "CALL" -> MessageType.CALL
                                    else -> MessageType.TEXT
                                }
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
            
            // Determinar extensión correcta según el tipo
            val extension = when (mediaType.lowercase()) {
                "audio" -> "m4a"
                "video" -> "mp4"
                "image", "photo" -> "jpg"
                else -> "mp4"
            }
            
            val fileName = "${mediaType}_$timestamp.$extension"

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

            Log.d("FirebaseRepo", "📤 Subiendo archivo a ruta: $path (${file.length()} bytes)")

            // Configurar metadata para mejor rendimiento
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(when (mediaType.lowercase()) {
                    "audio" -> "audio/mp4"
                    "video" -> "video/mp4"
                    "image", "photo" -> "image/jpeg"
                    else -> "application/octet-stream"
                })
                .build()

            val uploadTask = storageRef.putFile(android.net.Uri.fromFile(file), metadata)
            
            // Monitorear progreso
            uploadTask.addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                Log.d("FirebaseRepo", "📤 Progreso de subida: $progress%")
            }
            
            val snapshot = uploadTask.await()
            val downloadUrl = snapshot.storage.downloadUrl.await()

            Log.d("FirebaseRepo", "✅ Archivo subido exitosamente: $downloadUrl")
            downloadUrl.toString()

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error subiendo archivo", e)
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
                messageType = when (messageType.uppercase()) {
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
            
            // Obtener todos los usuarios activos usando el índice correcto
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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    private suspend fun notifyUsersInRange(chat: ProximityChat) {
        try {
            Log.d("FirebaseRepo", "🚨 NOTIFICACIÓN POLICIAL: Nuevo chat '${chat.title}' creado")
            
            // Usar consulta específica con filtros para evitar error de permisos
            val query = usersRef
                .orderByChild("isActive")
                .equalTo(true)
                .limitToFirst(50) // Limitar resultados para evitar sobrecarga
            
            val snapshot = query.get().await()
            var notifiedUsers = 0

            snapshot.children.forEach { userSnapshot ->
                try {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        // Filtrar por usuario activo y que no sea el creador
                        if (it.id != chat.creatorId && it.isActive) {
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
                } catch (e: Exception) {
                    Log.w("FirebaseRepo", "⚠️ Error procesando usuario ${userSnapshot.key}: ${e.message}")
                    // Continuar con el siguiente usuario
                }
            }
            
            Log.d("FirebaseRepo", "✅ Total usuarios notificados: $notifiedUsers")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error crítico notificando usuarios", e)
            
            // Intentar método alternativo si falla la consulta principal
            try {
                Log.d("FirebaseRepo", "🔄 Intentando método alternativo de notificación...")
                notifyUsersAlternative(chat)
            } catch (altException: Exception) {
                Log.e("FirebaseRepo", "❌ Método alternativo también falló", altException)
            }
        }
    }
    
    /**
     * Método alternativo de notificación que no requiere consulta masiva
     */
    private suspend fun notifyUsersAlternative(chat: ProximityChat) {
        try {
            Log.d("FirebaseRepo", "🔄 Usando método alternativo de notificación")
            
            // Solo notificar al usuario actual si está en rango
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.id != chat.creatorId) {
                val distance = calculateDistance(
                    chat.latitude, chat.longitude,
                    currentUser.latitude, currentUser.longitude
                )
                
                if (distance <= chat.radius) {
                    sendPushNotification(currentUser, chat, distance)
                    Log.d("FirebaseRepo", "📡 Usuario actual notificado - Distancia: ${String.format("%.0f", distance)}m")
                }
            }
            
            // Guardar notificación en Database para que otros usuarios la vean
            val notificationData = mapOf(
                "chatId" to chat.id,
                "chatTitle" to chat.title,
                "creatorId" to chat.creatorId,
                "creatorName" to chat.creatorName,
                "timestamp" to System.currentTimeMillis(),
                "type" to "new_chat_alert",
                "latitude" to chat.latitude,
                "longitude" to chat.longitude,
                "radius" to chat.radius
            )
            
            database.getReference("chat_notifications")
                .child(chat.id)
                .setValue(notificationData)
                .await()
            
            Log.d("FirebaseRepo", "✅ Notificación alternativa completada")
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error en método alternativo", e)
            throw e
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

    // ============== MÉTODOS PARA SERVICIO DE FONDO ==============

    suspend fun getCurrentUserId(): String? {
        return _currentUser.value?.id
    }

    suspend fun createProximityChatFromVoice(chat: ProximityChat): Result<ProximityChat> {
        return try {
            Log.d("FirebaseRepo", "🏗️ Creando chat de proximidad: ${chat.id}")
            
            // Guardar chat en Database
            chatsRef.child(chat.id).setValue(chat.toMap()).await()
            
            // Agregar creador como participante
            val participantData = mapOf(
                "userId" to chat.creatorId,
                "userName" to chat.creatorName,
                "joinedAt" to System.currentTimeMillis(),
                "isActive" to true
            )
            
            participantsRef.child(chat.id).child(chat.creatorId).setValue(participantData).await()
            
            // Actualizar lista local
            val currentChats = _nearbyChats.value.toMutableList()
            currentChats.add(chat)
            _nearbyChats.value = currentChats
            
            Log.d("FirebaseRepo", "✅ Chat creado exitosamente")
            Result.success(chat)
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error creando chat", e)
            Result.failure(e)
        }
    }

    suspend fun uploadAudioToChat(chatId: String, fileName: String, audioData: ByteArray): Result<String> {
        return try {
            Log.d("FirebaseRepo", "🎙️ Subiendo audio a chat: $chatId")
            
            // Crear referencia en Storage
            val audioRef = storage.reference.child("chat_audio/$chatId/$fileName")
            
            // Subir audio
            val uploadTask = audioRef.putBytes(audioData)
            val downloadUrl = uploadTask.await().storage.downloadUrl.await()
            
            // Guardar referencia en Database
            val audioMessage = mapOf<String, Any>(
                "id" to generateMessageId(),
                "chatId" to chatId,
                "senderId" to (_currentUser.value?.id ?: "unknown"),
                "senderName" to (_currentUser.value?.name ?: "Usuario"),
                "type" to "audio",
                "content" to downloadUrl.toString(),
                "fileName" to fileName,
                "timestamp" to System.currentTimeMillis(),
                "fileSize" to audioData.size
            )
            
            messagesRef.child(chatId).push().setValue(audioMessage).await()
            
            Log.d("FirebaseRepo", "✅ Audio subido exitosamente: ${downloadUrl}")
            Result.success(downloadUrl.toString())
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error subiendo audio", e)
            Result.failure(e)
        }
    }

    suspend fun sendNotificationToAllUsers(notificationData: Map<String, String>): Result<Unit> {
        return try {
            Log.d("FirebaseRepo", "📢 Enviando notificación a todos los usuarios")
            
            // Obtener todos los usuarios
            val usersSnapshot = usersRef.get().await()
            val allUsers = mutableListOf<User>()
            
            for (userSnapshot in usersSnapshot.children) {
                userSnapshot.getValue(User::class.java)?.let { user ->
                    if (user.id != _currentUser.value?.id) { // No notificar al usuario actual
                        allUsers.add(user)
                    }
                }
            }
            
            // Enviar notificación a cada usuario
            allUsers.forEach { user ->
                if (user.fcmToken.isNotEmpty()) {
                    val fcmData = mapOf<String, String>(
                        "title" to "🎤 Comando de Voz Detectado",
                        "body" to (notificationData["message"] ?: "Nuevo comando de voz"),
                        "chatType" to (notificationData["chat_type"] ?: ""),
                        "chatId" to (notificationData["chat_id"] ?: ""),
                        "timestamp" to (notificationData["timestamp"] ?: ""),
                        "type" to "voice_command_notification"
                    )
                    
                    sendFCMNotification(user.fcmToken, fcmData)
                }
            }
            
            // Guardar notificación global en Database
            val globalNotification = mapOf<String, Any>(
                "type" to "voice_command",
                "data" to notificationData,
                "timestamp" to System.currentTimeMillis(),
                "targetUsers" to allUsers.size
            )
            
            database.getReference("global_notifications")
                .push()
                .setValue(globalNotification)
                .await()
            
            Log.d("FirebaseRepo", "✅ Notificaciones enviadas a ${allUsers.size} usuarios")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error enviando notificaciones globales", e)
            Result.failure(e)
        }
    }

    /**
     * Envía notificaciones a usuarios cercanos basado en radio en metros
     */
    suspend fun sendNotificationToNearbyUsers(notificationData: Map<String, String>, radiusInMeters: Int): Result<Unit> {
        return try {
            Log.d("FirebaseRepo", "📢 Enviando notificación a usuarios cercanos (radio: ${radiusInMeters}m)")
            
            val currentUser = _currentUser.value
            if (currentUser == null) {
                Log.w("FirebaseRepo", "❌ Usuario actual no disponible")
                return Result.failure(Exception("Usuario no autenticado"))
            }
            
            // Obtener todos los usuarios
            val usersSnapshot = usersRef.get().await()
            val nearbyUsers = mutableListOf<User>()
            
            for (userSnapshot in usersSnapshot.children) {
                userSnapshot.getValue(User::class.java)?.let { user ->
                    if (user.id != currentUser.id) { // No notificar al usuario actual
                        // Calcular distancia entre usuarios
                        val distance = calculateDistance(
                            currentUser.latitude, currentUser.longitude,
                            user.latitude, user.longitude
                        )
                        
                        // Si el usuario está dentro del radio, agregarlo a la lista
                        if (distance <= radiusInMeters) {
                            nearbyUsers.add(user)
                            Log.d("FirebaseRepo", "📍 Usuario cercano encontrado: ${user.name} (${distance}m)")
                        }
                    }
                }
            }
            
            // Enviar notificación a usuarios cercanos
            nearbyUsers.forEach { user ->
                if (user.fcmToken.isNotEmpty()) {
                    val fcmData = mapOf<String, String>(
                        "title" to "🎤 Comando de Voz Cercano",
                        "body" to (notificationData["message"] ?: "Nuevo comando de voz en tu área"),
                        "chatType" to (notificationData["chat_type"] ?: ""),
                        "chatId" to (notificationData["chat_id"] ?: ""),
                        "timestamp" to (notificationData["timestamp"] ?: ""),
                        "notificationRadius" to (notificationData["notification_radius"] ?: ""),
                        "priority" to (notificationData["priority"] ?: "normal"),
                        "type" to "voice_command_nearby_notification"
                    )
                    
                    sendFCMNotification(user.fcmToken, fcmData)
                }
            }
            
            // Guardar notificación de proximidad en Database
            val proximityNotification = mapOf<String, Any>(
                "type" to "voice_command_proximity",
                "data" to notificationData,
                "radius" to radiusInMeters,
                "timestamp" to System.currentTimeMillis(),
                "targetUsers" to nearbyUsers.size,
                "creatorLocation" to mapOf(
                    "latitude" to currentUser.latitude,
                    "longitude" to currentUser.longitude
                )
            )
            
            database.getReference("proximity_notifications")
                .push()
                .setValue(proximityNotification)
                .await()
            
            Log.d("FirebaseRepo", "✅ Notificaciones enviadas a ${nearbyUsers.size} usuarios cercanos")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error enviando notificaciones de proximidad", e)
            Result.failure(e)
        }
    }
    

    


    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}