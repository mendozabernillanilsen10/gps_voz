package com.example.demoappchat.presentation.main

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.presentation.components.CreateChatDialog
import com.example.demoappchat.presentation.components.JoinChatDialog
import com.example.demoappchat.presentation.components.LocationPermissionDialog
import com.example.demoappchat.presentation.components.CompactRecordingIndicator
import com.example.demoappchat.presentation.recording.RecordingViewModel
import com.example.demoappchat.utils.LocationHelper
import com.example.demoappchat.data.service.ErrorLogger
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToChat: (String) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    recordingViewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val nearbyChats by viewModel.nearbyChats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isVoiceServiceEnabled by viewModel.isVoiceServiceEnabled.collectAsState()
    val recordingState by recordingViewModel.recordingState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    
    // Crear instancia de ErrorLogger
    val errorLogger = remember { ErrorLogger() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf<ProximityChat?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // Estados para funcionalidades de voz
    var discreteMode by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingType by remember { mutableStateOf("") }

    // Permisos de ubicación
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Permisos de audio para reconocimiento de voz
    val audioPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO
        )
    )

    // Solicitar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Estado para recordar si el usuario intentó activar el servicio
    var userTriedToEnable by remember { mutableStateOf(false) }

    // Habilitar servicio de voz automáticamente cuando se concedan permisos de audio
    LaunchedEffect(audioPermissions.allPermissionsGranted, userTriedToEnable) {
        if (audioPermissions.allPermissionsGranted && userTriedToEnable && !isVoiceServiceEnabled) {
            // Activar el servicio automáticamente después de obtener permisos
            viewModel.toggleVoiceService(true)
            userTriedToEnable = false
        }
    }

    // Inicializar ubicación con delay para evitar bloqueos
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            // Pequeño delay para evitar bloqueos en el inicio
            kotlinx.coroutines.delay(500)
            LocationHelper.getCurrentLocation(context) { location ->
                viewModel.updateLocation(location)
            }
        }
    }

    // Navegar a chat creado/unido con manejo mejorado para Honor y logging completo
    LaunchedEffect(uiState.createdChatId, uiState.joinedChatId) {
        uiState.createdChatId?.let { chatId ->
            Log.d("MainScreen", "🔄 Navegando a chat creado: $chatId")
            
            // Log inicial para Honor
           // if (ErrorLogger.isHonorDevice()) {
                errorLogger.logHonorSpecificIssue(
                    issue = "navigation_attempt_start",
                    context = "MainScreen.LaunchedEffect.createdChatId",
                    additionalData = mapOf(
                        "chat_id" to chatId,
                        "ui_state_loading" to uiState.isLoading,
                        "navigation_type" to "created_chat"
                    )
                )
           // }
            
            try {
                // Delay específico para Honor devices
                val delay = if (ErrorLogger.isHonorDevice()) 300L else 200L
                kotlinx.coroutines.delay(delay)
                
                // Verificar que el chatId sigue siendo válido después del delay
                if (uiState.createdChatId == chatId) {
                    onNavigateToChat(chatId)
                    viewModel.clearNavigationEvents()
                    Log.d("MainScreen", "✅ Navegación exitosa a chat: $chatId")
                    
                    // Log éxito para Honor
                   // if (ErrorLogger.isHonorDevice()) {
                        errorLogger.logHonorSpecificIssue(
                            issue = "navigation_success",
                            context = "MainScreen.LaunchedEffect.createdChatId",
                            additionalData = mapOf(
                                "chat_id" to chatId,
                                "delay_used" to delay,
                                "attempt" to "first"
                            )
                        )
                  //  }
                } else {
                    Log.w("MainScreen", "⚠️ ChatId cambió durante el delay: $chatId vs ${uiState.createdChatId}")
                    
                    //if (ErrorLogger.isHonorDevice()) {
                        errorLogger.logHonorSpecificIssue(
                            issue = "chatid_changed_during_delay",
                            context = "MainScreen.LaunchedEffect.createdChatId",
                            additionalData = mapOf(
                                "original_chat_id" to chatId,
                                "current_chat_id" to (uiState.createdChatId ?: "null"),
                                "delay_used" to delay
                            )
                        )
                   // }
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "❌ Error navegando a chat creado: $chatId", e)
                
                // Log error para Firebase
                errorLogger.logNavigationError(
                    fromScreen = "main",
                    toScreen = "chat",
                    chatId = chatId,
                    throwable = e,
                    additionalData = mapOf(
                        "navigation_type" to "created_chat",
                        "attempt" to "first",
                        "device_brand" to android.os.Build.BRAND,
                        "device_model" to android.os.Build.MODEL
                    )
                )
                
                // Reintentar después de un delay más largo
                kotlinx.coroutines.delay(800)
                try {
                    Log.d("MainScreen", "🔄 Reintentando navegación a chat: $chatId")
                    onNavigateToChat(chatId)
                    viewModel.clearNavigationEvents()
                    Log.d("MainScreen", "✅ Navegación exitosa en reintento: $chatId")
                    
                    //if (ErrorLogger.isHonorDevice()) {
                        errorLogger.logHonorSpecificIssue(
                            issue = "navigation_success_retry",
                            context = "MainScreen.LaunchedEffect.createdChatId",
                            additionalData = mapOf(
                                "chat_id" to chatId,
                                "attempt" to "retry"
                            )
                        )
                   // }
                } catch (retryError: Exception) {
                    Log.e("MainScreen", "❌ Error en reintento de navegación: $chatId", retryError)
                    
                    errorLogger.logNavigationError(
                        fromScreen = "main",
                        toScreen = "chat",
                        chatId = chatId,
                        throwable = retryError,
                        additionalData = mapOf(
                            "navigation_type" to "created_chat",
                            "attempt" to "retry_failed",
                            "device_brand" to android.os.Build.BRAND,
                            "device_model" to android.os.Build.MODEL,
                            "original_error" to (e.message ?: "unknown")
                        )
                    )
                    
                    // Limpiar el estado para evitar bucles infinitos
                    viewModel.clearNavigationEvents()
                }
            }
        }
        
        uiState.joinedChatId?.let { chatId ->
            Log.d("MainScreen", "🔄 Navegando a chat unido: $chatId")
            
         //   if (ErrorLogger.isHonorDevice()) {
                errorLogger.logHonorSpecificIssue(
                    issue = "navigation_attempt_start",
                    context = "MainScreen.LaunchedEffect.joinedChatId",
                    additionalData = mapOf(
                        "chat_id" to chatId,
                        "navigation_type" to "joined_chat"
                    )
                )
        //    }
            
            try {
                val delay = if (ErrorLogger.isHonorDevice()) 300L else 200L
                kotlinx.coroutines.delay(delay)
                
                if (uiState.joinedChatId == chatId) {
                    onNavigateToChat(chatId)
                    viewModel.clearNavigationEvents()
                    Log.d("MainScreen", "✅ Navegación exitosa a chat: $chatId")
                    
                   // if (ErrorLogger.isHonorDevice()) {
                        errorLogger.logHonorSpecificIssue(
                            issue = "navigation_success",
                            context = "MainScreen.LaunchedEffect.joinedChatId",
                            additionalData = mapOf(
                                "chat_id" to chatId,
                                "delay_used" to delay,
                                "attempt" to "first"
                            )
                        )
                   // }
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "❌ Error navegando a chat unido: $chatId", e)
                
                errorLogger.logNavigationError(
                    fromScreen = "main",
                    toScreen = "chat",
                    chatId = chatId,
                    throwable = e,
                    additionalData = mapOf(
                        "navigation_type" to "joined_chat",
                        "attempt" to "first"
                    )
                )
                
                kotlinx.coroutines.delay(800)
                try {
                    onNavigateToChat(chatId)
                    viewModel.clearNavigationEvents()
                } catch (retryError: Exception) {
                    Log.e("MainScreen", "❌ Error en reintento de navegación", retryError)
                    errorLogger.logNavigationError(
                        fromScreen = "main",
                        toScreen = "chat",
                        chatId = chatId,
                        throwable = retryError,
                        additionalData = mapOf(
                            "navigation_type" to "joined_chat",
                            "attempt" to "retry_failed"
                        )
                    )
                    viewModel.clearNavigationEvents()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SafeVoice",
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Indicador de grabación compacto
                    if (recordingState.isRecording) {
                        CompactRecordingIndicator(
                            isRecording = recordingState.isRecording,
                            recordingTime = recordingState.recordingTime
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (isVoiceServiceEnabled) {
                        // Indicador de voz activa cuando no está grabando
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voz activa",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(onClick = { onNavigateToSettings() }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menú",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Perfil") },
                                onClick = {
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cerrar Sesión") },
                                onClick = {
                                    showMenu = false
                                    onSignOut()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (locationPermissions.allPermissionsGranted && currentLocation != null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Crear Chat",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) { paddingValues ->

        if (!locationPermissions.allPermissionsGranted) {
            LocationPermissionDialog(
                onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header minimalista
                item {
                    currentUser?.let { user ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar minimalista
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 20.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Título principal
                            Text(
                                text = "Hola, ${user.name}",
                                fontWeight = FontWeight.Normal,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Subtítulo
                            Text(
                                text = "Sistema activo",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Indicadores de estado minimalistas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ubicación
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Ubicación",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                        
                        // Chats disponibles
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${nearbyChats.size}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Estado del servicio de voz minimalista
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isVoiceServiceEnabled) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Servicio de voz",
                                    tint = if (isVoiceServiceEnabled) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Detección de Voz",
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isVoiceServiceEnabled) 
                                            "Activo" 
                                        else 
                                            "Inactivo",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                            
                            Switch(
                                checked = isVoiceServiceEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !audioPermissions.allPermissionsGranted) {
                                        userTriedToEnable = true
                                        audioPermissions.launchMultiplePermissionRequest()
                                    } else {
                                        viewModel.toggleVoiceService(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }

                // Chats section header
                if (nearbyChats.isNotEmpty()) {
                    item {
                        Text(
                            text = "Chats disponibles",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Chat items
                if (nearbyChats.isEmpty()) {
                    item {
                        EmptyStateContent(
                            onCreateChat = { showCreateDialog = true } // Pasar la misma función que usa el FAB
                        )
                    }
                } else {
                    items(
                        items = nearbyChats,
                        key = { chat -> 
                            if (chat.id.isNotBlank()) {
                                chat.id
                            } else {
                                "${chat.createdAt}_${chat.creatorId}"
                            }
                        }
                    ) { chat ->
                        ModernChatCard(
                            chat = chat,
                            userLocation = currentLocation,
                            onClick = { showJoinDialog = chat }
                        )
                    }
                }
            }
        }
    }

    // Diálogos
    if (showCreateDialog) {
        CreateChatDialog(
            onDismiss = { showCreateDialog = false },
            onCreateChat = { chat ->
                currentUser?.let { user ->
                    currentLocation?.let { location ->
                        val newChat = chat.copy(
                            creatorId = user.id,
                            creatorName = user.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.createChat(newChat)
                    }
                }
                showCreateDialog = false
            }
        )
    }

    showJoinDialog?.let { chat ->
        JoinChatDialog(
            chat = chat,
            onDismiss = { showJoinDialog = null },
            onJoinChat = { pin ->
                viewModel.joinChat(chat.id, pin) { success ->
                    if (!success) {
                        // TODO: Mostrar SnackBar con error
                    }
                }
                showJoinDialog = null
            }
        )
    }
}

@Composable
fun ModernChatCard(
    chat: ProximityChat,
    userLocation: android.location.Location?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header minimalista
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar minimalista
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.creatorName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Información del usuario
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.creatorName,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Categoría y distancia en una línea
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge de categoría minimalista
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = when (chat.category) {
                                    "emergency" -> "Emergencia"
                                    "security" -> "Seguridad"
                                    "traffic" -> "Tráfico"
                                    "community" -> "Comunidad"
                                    else -> "General"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        
                        // Distancia
                        Text(
                            text = userLocation?.let {
                                chat.getDistanceText(it.latitude, it.longitude)
                            } ?: "Calculando...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Botón de unirse minimalista
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Unirse",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Contenido del chat
            Text(
                text = chat.title,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
            
            if (chat.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = chat.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Footer minimalista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${chat.participantsCount} participantes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "Hace ${((System.currentTimeMillis() - chat.createdAt) / (1000 * 60)).toInt()}min",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent(
    onCreateChat: () -> Unit // Agregar este parámetro
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono minimalista
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Forum,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No hay chats disponibles",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Crea el primer chat en tu zona para conectar con personas cercanas",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            onClick = onCreateChat, // Usar la función pasada como parámetro
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Crear chat",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}