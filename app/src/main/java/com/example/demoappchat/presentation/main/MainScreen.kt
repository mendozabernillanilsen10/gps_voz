package com.example.demoappchat.presentation.main

import android.Manifest
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
import androidx.compose.ui.graphics.Brush
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
import com.example.demoappchat.ui.theme.EmergencyRed
import com.example.demoappchat.ui.theme.SafetyGreen
import com.example.demoappchat.ui.theme.WarningOrange
import com.example.demoappchat.utils.LocationHelper

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

    // Inicializar ubicación
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            LocationHelper.getCurrentLocation(context) { location ->
                viewModel.updateLocation(location)
            }
        }
    }

    // Navegar a chat creado/unido
    LaunchedEffect(uiState.createdChatId, uiState.joinedChatId) {
        uiState.createdChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.clearNavigationEvents()
        }
        uiState.joinedChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.clearNavigationEvents()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SafeVoice",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1877F2) // Facebook blue
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
                            color = Color(0xFF42C85F), // WhatsApp green
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voz activa",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(onClick = { onNavigateToSettings() }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = Color.White
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menú",
                                tint = Color.White
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
                    containerColor = EmergencyRed,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Crear Chat",
                        modifier = Modifier.size(32.dp)
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
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con información del usuario modernizado
                item {
                    currentUser?.let { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar mejorado con gradiente simulado
                                    Box(
                                        modifier = Modifier.size(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmergencyRed,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = user.name.firstOrNull()?.toString()?.uppercase() ?: "U",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hola, ${user.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Sistema de emergencias activo",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Stats row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Ubicación
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = SafetyGreen.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = SafetyGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Ubicación OK",
                                                color = SafetyGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    // Chats disponibles
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = EmergencyRed.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = EmergencyRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${nearbyChats.size} Chats",
                                                color = EmergencyRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Estado del servicio de voz simplificado
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVoiceServiceEnabled) SafetyGreen.copy(alpha = 0.1f) else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Servicio de voz",
                                    tint = if (isVoiceServiceEnabled) SafetyGreen else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Detección de Voz",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = when {
                                            isVoiceServiceEnabled -> "Escuchando comandos de emergencia"
                                            !audioPermissions.allPermissionsGranted -> "Permisos de micrófono requeridos"
                                            else -> "Toca para activar"
                                        },
                                        fontSize = 14.sp,
                                        color = when {
                                            isVoiceServiceEnabled -> SafetyGreen
                                            !audioPermissions.allPermissionsGranted -> WarningOrange
                                            else -> Color.Gray
                                        }
                                    )
                                }
                            }
                            
                            Switch(
                                checked = isVoiceServiceEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !audioPermissions.allPermissionsGranted) {
                                        // Marcar que el usuario intentó activar y solicitar permisos
                                        userTriedToEnable = true
                                        audioPermissions.launchMultiplePermissionRequest()
                                    } else {
                                        viewModel.toggleVoiceService(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SafetyGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                // Section divider
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Chats section header
                if (nearbyChats.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chats de Emergencia",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1E21)
                                )
                                
                                Text(
                                    text = "${nearbyChats.size} activos",
                                    fontSize = 14.sp,
                                    color = Color(0xFF65676B)
                                )
                            }
                        }
                    }
                }

                // Chat items
                if (nearbyChats.isEmpty()) {
                    item {
                        EmptyStateContent()
                    }
                } else {
                    items(
                        items = nearbyChats,
                        key = { chat -> 
                            // Use a combination of fields to ensure uniqueness
                            // If id is empty, use createdAt + creatorId as fallback
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header estilo Instagram/Facebook
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circular con gradiente
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        color = when (chat.category) {
                            "emergency" -> Color(0xFFE53E3E)
                            "security" -> Color(0xFFFF6B35)
                            else -> Color(0xFF1877F2)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chat.creatorName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Información del usuario
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.creatorName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge de categoría
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (chat.category) {
                                "emergency" -> Color(0xFFE53E3E).copy(alpha = 0.1f)
                                "security" -> Color(0xFFFF6B35).copy(alpha = 0.1f)
                                else -> Color(0xFF1877F2).copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = when (chat.category) {
                                    "emergency" -> "🚨 Emergencia"
                                    "security" -> "🔒 Seguridad"
                                    else -> "💬 General"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                color = when (chat.category) {
                                    "emergency" -> Color(0xFFE53E3E)
                                    "security" -> Color(0xFFFF6B35)
                                    else -> Color(0xFF1877F2)
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Distancia
                        Text(
                            text = userLocation?.let {
                                chat.getDistanceText(it.latitude, it.longitude)
                            } ?: "Calculando...",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B)
                        )
                    }
                }
                
                // Botón de unirse estilo moderno
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1877F2)
                ) {
                    Text(
                        text = "Unirse",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contenido del chat
            Text(
                text = chat.title,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color(0xFF1C1E21),
                lineHeight = 22.sp
            )
            
            if (chat.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = chat.description,
                    fontSize = 14.sp,
                    color = Color(0xFF65676B),
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer con estadísticas
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
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${chat.participantsCount} participantes",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )
                }
                
                Text(
                    text = "Hace ${((System.currentTimeMillis() - chat.createdAt) / (1000 * 60)).toInt()}min",
                    fontSize = 13.sp,
                    color = Color(0xFF65676B)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFFDADADA))
            )
        }
    }
}

@Composable
fun EmptyStateContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern empty illustration
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF0F2F5),
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(24.dp),
                        tint = Color(0xFF65676B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No hay chats en tu área",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1E21),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Crea el primer chat de emergencia en tu zona y conecta con personas cercanas",
                fontSize = 16.sp,
                color = Color(0xFF65676B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1877F2)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crear primer chat",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}