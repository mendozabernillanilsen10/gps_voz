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
import com.example.demoappchat.presentation.components.VoiceRecordingSettings
import com.example.demoappchat.ui.theme.InfoBlue
import com.example.demoappchat.ui.theme.MinimalistPurple

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVoiceCommands: () -> Unit = {},
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
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
                    IntegratedVoiceCommandsSettings(
                        commandActions = uiState.commandActions,
                        voiceSensitivity = uiState.voiceSensitivity,
                        stealthMode = uiState.stealthMode,
                        onAddCommand = { command, action ->
                            viewModel.setCommandAction(command, action)
                        },
                        onRemoveCommand = { command ->
                            viewModel.removeCommandAction(command)
                        },
                        onUpdateCommandAction = { command, action ->
                            viewModel.setCommandAction(command, action)
                        },
                        onSensitivityChange = { sensitivity ->
                            viewModel.setVoiceSensitivity(sensitivity)
                        },
                        onStealthModeToggle = { enabled ->
                            viewModel.toggleStealthMode(enabled)
                        }
                    )
                }
            }
            
            // Voice Recording Settings Section
            item {
                SettingsSection(
                    title = "Configuración de Grabación",
                    subtitle = "Controla duración y tipo de contenido"
                ) {
                    VoiceRecordingSettings(
                        audioDuration = uiState.audioRecordingDuration,
                        videoDuration = uiState.videoRecordingDuration,
                        photoCaptureEnabled = uiState.photoCaptureEnabled,
                        autoSendEnabled = uiState.autoSendRecordings,
                        recordingQuality = uiState.recordingQuality,
                        commandActions = uiState.commandActions,
                        onAudioDurationChange = { duration ->
                            viewModel.setAudioRecordingDuration(duration)
                        },
                        onVideoDurationChange = { duration ->
                            viewModel.setVideoRecordingDuration(duration)
                        },
                        onPhotoCaptureToggle = { enabled ->
                            viewModel.setPhotoCaptureEnabled(enabled)
                        },
                        onAutoSendToggle = { enabled ->
                            viewModel.setAutoSendRecordings(enabled)
                        },
                        onQualityChange = { quality ->
                            viewModel.setRecordingQuality(quality)
                        },
                        onCommandActionChange = { command, action ->
                            viewModel.setCommandAction(command, action)
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
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEnabled) "Escuchando comandos" else "Servicio desactivado",
                    fontSize = 14.sp,
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Funciona sin notificaciones visibles",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = discreteMode,
                    onCheckedChange = onDiscreteModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
        
        // Permissions Status
        if (!audioPermissions.allPermissionsGranted) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MinimalistPurple.copy(alpha = 0.1f)
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
                        tint = MinimalistPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Se requieren permisos de audio y cámara",
                        fontSize = 14.sp,
                        color = MinimalistPurple
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
    onCommandRemove: (String) -> Unit,
    onNavigateToCommands: () -> Unit = {}
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            customCommands.forEach { command ->
                CustomCommandItem(
                    command = command,
                    onRemove = { onCommandRemove(command) }
                )
            }
        }
        
        // Navigate to commands screen button
        Button(
            onClick = onNavigateToCommands,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Configurar Comandos de Voz",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Add command button
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
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
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = description,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "\"$text\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
        color = MaterialTheme.colorScheme.surfaceVariant
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
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${radius.toInt()} km",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
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
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 km",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "100 km",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Descripción informativa
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Radio actual: ${radius.toInt()} km. Los agentes dentro de este rango recibirán alertas automáticamente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface
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
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sube grabaciones automáticamente al activarse",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = autoUpload,
                onCheckedChange = onAutoUploadToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        
        // Emergency Info
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = InfoBlue.copy(alpha = 0.1f)
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
                    tint = InfoBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Las grabaciones se guardan en Firebase Storage con cifrado",
                    fontSize = 14.sp,
                    color = InfoBlue
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Permite que otros vean tu ubicación en emergencias",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = shareLocation,
                onCheckedChange = onShareLocationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        
        // Data Retention
        Column {
            Text(
                text = "Retención de Datos",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Días para mantener grabaciones: $dataRetention",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = dataRetention.toFloat(),
                onValueChange = { onDataRetentionChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onPrimary,
                    activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 día",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "30 días",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}