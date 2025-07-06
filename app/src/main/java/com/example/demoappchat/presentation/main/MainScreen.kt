package com.example.demoappchat.presentation.main

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.demoappchat.ui.theme.EmergencyRed
import com.example.demoappchat.ui.theme.SafetyGreen
import com.example.demoappchat.ui.theme.WarningOrange
import com.example.demoappchat.utils.LocationHelper

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToChat: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val nearbyChats by viewModel.nearbyChats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf<ProximityChat?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // Permisos de ubicación
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Solicitar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
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
                    Text(
                        "SecurityChat",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyRed
                ),
                actions = {
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
                    containerColor = EmergencyRed
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Crear Chat",
                        tint = Color.White
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header con información del usuario
                currentUser?.let { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SafetyGreen.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(SafetyGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.firstOrNull()?.toString() ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "¡Hola, ${user.name}!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Ubicación activa • ${nearbyChats.size} chats cercanos",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Lista de chats cercanos
                if (nearbyChats.isEmpty()) {
                    EmptyStateContent()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Chats de Seguridad Cercanos",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(nearbyChats) { chat ->
                            ChatCard(
                                chat = chat,
                                userLocation = currentLocation,
                                onClick = { showJoinDialog = chat }
                            )
                        }
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
                        // Mostrar error de PIN incorrecto
                    }
                }
                showJoinDialog = null
            }
        )
    }
}

@Composable
fun ChatCard(
    chat: ProximityChat,
    userLocation: android.location.Location?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = when (chat.category) {
                                "emergency" -> EmergencyRed
                                "security" -> WarningOrange
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = chat.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = chat.description,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = userLocation?.let {
                                chat.getDistanceText(it.latitude, it.longitude)
                            } ?: "Calculando...",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "${chat.participantsCount} participantes",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Por ${chat.creatorName}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Unirse",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No hay chats cercanos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Text(
            text = "Sé el primero en crear un chat de seguridad\nen tu área",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Presiona el botón + para crear un chat",
            fontSize = 12.sp,
            color = EmergencyRed,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}