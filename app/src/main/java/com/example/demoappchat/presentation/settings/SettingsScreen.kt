package com.example.demoappchat.presentation.settings

import android.Manifest
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Voice commands state
    var customCommands by remember { mutableStateOf(uiState.voiceCommands) }
    var newCommand by remember { mutableStateOf("") }
    var showAddCommandDialog by remember { mutableStateOf(false) }
    
    // Audio permissions
    val audioPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.FOREGROUND_SERVICE
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1877F2)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F2F5)),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Voice Service Settings Section
            item {
                SettingsSection(
                    title = "Servicio de Voz",
                    subtitle = "Configuración de comandos y detección"
                ) {
                    VoiceServiceSettings(
                        isEnabled = uiState.isVoiceServiceEnabled,
                        discreteMode = uiState.discreteMode,
                        onServiceToggle = { viewModel.toggleVoiceService(it) },
                        onDiscreteModeToggle = { viewModel.toggleDiscreteMode(it) },
                        audioPermissions = audioPermissions
                    )
                }
            }
            
            // Voice Commands Section
            item {
                SettingsSection(
                    title = "Comandos de Voz",
                    subtitle = "Personaliza los comandos de activación"
                ) {
                    VoiceCommandsSettings(
                        commands = uiState.voiceCommands,
                        onCommandAdd = { command ->
                            viewModel.addVoiceCommand(command)
                        },
                        onCommandRemove = { command ->
                            viewModel.removeVoiceCommand(command)
                        }
                    )
                }
            }
            
            // Audio Transmission Settings
            item {
                SettingsSection(
                    title = "Transmisión de Audio",
                    subtitle = "Configuración de alcance y calidad"
                ) {
                    AudioTransmissionSettings(
                        radius = uiState.transmissionRadius,
                        quality = uiState.audioQuality,
                        onRadiusChange = { viewModel.updateTransmissionRadius(it) },
                        onQualityChange = { viewModel.updateAudioQuality(it) }
                    )
                }
            }
            
            // Emergency Settings
            item {
                SettingsSection(
                    title = "Configuración de Emergencia",
                    subtitle = "Ajustes para situaciones críticas"
                ) {
                    EmergencySettings(
                        autoUpload = uiState.autoUploadRecordings,
                        emergencyContacts = uiState.emergencyContacts,
                        onAutoUploadToggle = { viewModel.toggleAutoUpload(it) },
                        onEmergencyContactsChange = { viewModel.updateEmergencyContacts(it) }
                    )
                }
            }
            
            // Privacy Settings
            item {
                SettingsSection(
                    title = "Privacidad y Seguridad",
                    subtitle = "Control de datos y permisos"
                ) {
                    PrivacySettings(
                        shareLocation = uiState.shareLocation,
                        dataRetention = uiState.dataRetentionDays,
                        onShareLocationToggle = { viewModel.toggleShareLocation(it) },
                        onDataRetentionChange = { viewModel.updateDataRetention(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1C1E21)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFF65676B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceServiceSettings(
    isEnabled: Boolean,
    discreteMode: Boolean,
    onServiceToggle: (Boolean) -> Unit,
    onDiscreteModeToggle: (Boolean) -> Unit,
    audioPermissions: com.google.accompanist.permissions.MultiplePermissionsState
) {
    var pendingServiceEnable by remember { mutableStateOf(false) }
    
    // Monitor permission changes and enable service if permissions were just granted
    LaunchedEffect(audioPermissions.allPermissionsGranted) {
        if (audioPermissions.allPermissionsGranted && pendingServiceEnable) {
            onServiceToggle(true)
            pendingServiceEnable = false
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Service Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activar Servicio de Voz",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = if (isEnabled) "Escuchando comandos" else "Servicio desactivado",
                    fontSize = 14.sp,
                    color = if (isEnabled) Color(0xFF42C85F) else Color(0xFF65676B)
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !audioPermissions.allPermissionsGranted) {
                        pendingServiceEnable = true
                        audioPermissions.launchMultiplePermissionRequest()
                    } else {
                        onServiceToggle(enabled)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF42C85F),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE4E6EA)
                )
            )
        }
        
        // Discrete Mode
        if (isEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Modo Discreto",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Text(
                        text = "Funciona sin notificaciones visibles",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B)
                    )
                }
                
                Switch(
                    checked = discreteMode,
                    onCheckedChange = onDiscreteModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1877F2),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE4E6EA)
                    )
                )
            }
        }
        
        // Permissions Status
        if (!audioPermissions.allPermissionsGranted) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF3CD)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Se requieren permisos de audio y cámara",
                        fontSize = 14.sp,
                        color = Color(0xFF856404)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceCommandsSettings(
    commands: List<String>,
    onCommandAdd: (String) -> Unit,
    onCommandRemove: (String) -> Unit
) {
    var newCommand by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Predefined commands
        val predefinedCommands = listOf(
            "óyeme" to "📢 Audio",
            "alerta" to "🚨 Emergencia",
            "grabar video" to "📹 Video",
            "ayuda" to "🆘 Socorro"
        )
        
        predefinedCommands.forEach { (command, description) ->
            CommandChip(
                text = command,
                description = description,
                isSelected = commands.contains(command),
                onToggle = { 
                    if (commands.contains(command)) {
                        onCommandRemove(command)
                    } else {
                        onCommandAdd(command)
                    }
                }
            )
        }
        
        // Custom commands
        val customCommands = commands.filter { cmd ->
            predefinedCommands.none { it.first == cmd }
        }
        
        if (customCommands.isNotEmpty()) {
            Text(
                text = "Comandos Personalizados",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF65676B)
            )
            
            customCommands.forEach { command ->
                CustomCommandItem(
                    command = command,
                    onRemove = { onCommandRemove(command) }
                )
            }
        }
        
        // Add command button
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF1877F2)
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar Comando Personalizado")
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Comando") },
            text = {
                OutlinedTextField(
                    value = newCommand,
                    onValueChange = { newCommand = it },
                    label = { Text("Comando de voz") },
                    placeholder = { Text("Ej: emergencia casa") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCommand.isNotBlank()) {
                            onCommandAdd(newCommand.trim())
                            newCommand = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CommandChip(
    text: String,
    description: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1877F2) else Color(0xFFF0F2F5)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = description,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color(0xFF65676B)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "\"$text\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF1C1E21)
            )
        }
    }
}

@Composable
fun CustomCommandItem(
    command: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0F2F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\"$command\"",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF1C1E21)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = Color(0xFF65676B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AudioTransmissionSettings(
    radius: Float,
    quality: String,
    onRadiusChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transmission Radius
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Radio de Transmisión",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1E21)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1877F2)
                ) {
                    Text(
                        text = "${radius.toInt()} km",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botones de acceso rápido para distancias comunes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val commonDistances = listOf(1f, 5f, 10f, 25f, 50f, 100f)
                commonDistances.forEach { distance ->
                    val isSelected = radius == distance
                    FilterChip(
                        onClick = { onRadiusChange(distance) },
                        label = { 
                            Text(
                                text = "${distance.toInt()}km",
                                fontSize = 12.sp
                            ) 
                        },
                        selected = isSelected,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1877F2),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFE4E6EA),
                            labelColor = Color(0xFF65676B)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = radius,
                onValueChange = onRadiusChange,
                valueRange = 1f..100f,
                steps = 98, // 99 pasos para tener valores enteros de 1 a 100
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1877F2),
                    activeTrackColor = Color(0xFF1877F2),
                    inactiveTrackColor = Color(0xFFE4E6EA)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 km",
                    fontSize = 12.sp,
                    color = Color(0xFF65676B)
                )
                Text(
                    text = "100 km",
                    fontSize = 12.sp,
                    color = Color(0xFF65676B)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Descripción informativa
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F2F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Radio actual: ${radius.toInt()} km. Los agentes dentro de este rango recibirán alertas automáticamente.",
                        fontSize = 12.sp,
                        color = Color(0xFF65676B),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        
        // Audio Quality
        Column {
            Text(
                text = "Calidad de Audio",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF1C1E21)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val qualities = listOf("Baja", "Media", "Alta")
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                qualities.forEach { qualityOption ->
                    FilterChip(
                        selected = quality == qualityOption,
                        onClick = { onQualityChange(qualityOption) },
                        label = { Text(qualityOption) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1877F2),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencySettings(
    autoUpload: Boolean,
    emergencyContacts: List<String>,
    onAutoUploadToggle: (Boolean) -> Unit,
    onEmergencyContactsChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Auto Upload
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Subida Automática",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = "Sube grabaciones automáticamente al activarse",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B)
                )
            }
            
            Switch(
                checked = autoUpload,
                onCheckedChange = onAutoUploadToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF42C85F),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE4E6EA)
                )
            )
        }
        
        // Emergency Info
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE3F2FD)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Las grabaciones se guardan en Firebase Storage con cifrado",
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@Composable
fun PrivacySettings(
    shareLocation: Boolean,
    dataRetention: Int,
    onShareLocationToggle: (Boolean) -> Unit,
    onDataRetentionChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Share Location
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compartir Ubicación",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = "Permite que otros vean tu ubicación en emergencias",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B)
                )
            }
            
            Switch(
                checked = shareLocation,
                onCheckedChange = onShareLocationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF1877F2),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE4E6EA)
                )
            )
        }
        
        // Data Retention
        Column {
            Text(
                text = "Retención de Datos",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF1C1E21)
            )
            Text(
                text = "Días para mantener grabaciones: $dataRetention",
                fontSize = 14.sp,
                color = Color(0xFF65676B)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = dataRetention.toFloat(),
                onValueChange = { onDataRetentionChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1877F2),
                    activeTrackColor = Color(0xFF1877F2),
                    inactiveTrackColor = Color(0xFFE4E6EA)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 día",
                    fontSize = 12.sp,
                    color = Color(0xFF65676B)
                )
                Text(
                    text = "30 días",
                    fontSize = 12.sp,
                    color = Color(0xFF65676B)
                )
            }
        }
    }
}