package com.example.demoappchat.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.presentation.settings.AddCommandDialogIntegrated
import com.example.demoappchat.presentation.settings.EditCommandDialogIntegrated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedVoiceCommandsSettings(
    commandActions: Map<String, String>,
    onAddCommand: (String, String) -> Unit,
    onRemoveCommand: (String) -> Unit,
    onUpdateCommandAction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCommand by remember { mutableStateOf<String?>(null) }
    var editingAction by remember { mutableStateOf("AUDIO") }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Comandos predefinidos con sus acciones por defecto
    val predefinedCommands = mapOf(
        "óyeme" to "AUDIO",
        "alerta" to "TEXT", 
        "grabar video" to "VIDEO",
        "ayuda" to "LOCATION",
        "foto" to "PHOTO",
        "emergencia" to "CALL"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Comandos predefinidos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Comandos Predefinidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1E21)
                )
                
                Text(
                    text = "Comandos de voz estándar con acciones predefinidas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF65676B)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = predefinedCommands.toList(),
                        key = { (command, _) -> command }
                    ) { (command, action) ->
                        val currentAction = commandActions[command] ?: action
                        PredefinedCommandChip(
                            command = command,
                            action = currentAction,
                            onClick = {
                                editingCommand = command
                                editingAction = currentAction
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Comandos personalizados
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comandos Personalizados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1E21)
                    )
                    
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF1877F2).copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar comando",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (commandActions.isEmpty()) {
                    Text(
                        text = "No hay comandos personalizados configurados",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF65676B)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commandActions.toList().forEach { (command, action) ->
                            CommandCard(
                                command = command,
                                action = action,
                                onRemove = { onRemoveCommand(command) },
                                onEdit = {
                                    editingCommand = command
                                    editingAction = action
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog para agregar nuevo comando
    if (showAddDialog) {
        AddCommandDialogIntegrated(
            onDismiss = { showAddDialog = false },
            onConfirm = { command, action ->
                if (command.isNotBlank()) {
                    onAddCommand(command.trim(), action)
                    showAddDialog = false
                }
            }
        )
    }
    
    // Dialog para editar comando
    if (showEditDialog && editingCommand != null) {
        EditCommandDialogIntegrated(
            command = editingCommand!!,
            currentAction = editingAction,
            onDismiss = { showEditDialog = false },
            onConfirm = { action ->
                onUpdateCommandAction(editingCommand!!, action)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun PredefinedCommandChip(
    command: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (action) {
                "AUDIO" -> Color(0xFFE3F2FD)
                "VIDEO" -> Color(0xFFFFEBEE)
                "PHOTO" -> Color(0xFFE8F5E8)
                "TEXT" -> Color(0xFFFFF3E0)
                "LOCATION" -> Color(0xFFF5F5F5)
                "CALL" -> Color(0xFFE3F2FD)
                else -> Color(0xFFF5F5F5)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Icono según la acción
            when (action) {
                "AUDIO" -> {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "VIDEO" -> {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "PHOTO" -> {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "TEXT" -> {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "LOCATION" -> {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(16.dp)
                    )
                }
                "CALL" -> {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = "\"$command\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = when (action) {
                        "AUDIO" -> "Grabar Audio"
                        "VIDEO" -> "Grabar Video"
                        "PHOTO" -> "Tomar Foto"
                        "TEXT" -> "Enviar Texto"
                        "LOCATION" -> "Enviar Ubicación"
                        "CALL" -> "Llamada Automática"
                        else -> action
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (action) {
                        "AUDIO" -> Color(0xFF1976D2)
                        "VIDEO" -> Color(0xFFD32F2F)
                        "PHOTO" -> Color(0xFF388E3C)
                        "TEXT" -> Color(0xFFF57C00)
                        "LOCATION" -> Color(0xFF616161)
                        "CALL" -> Color(0xFF1976D2)
                        else -> Color(0xFF616161)
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CommandCard(
    command: String,
    action: String,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"$command\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1C1E21)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (action) {
                        "AUDIO" -> {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Grabar Audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "VIDEO" -> {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Grabar Video",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "PHOTO" -> {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF388E3C),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Tomar Foto",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF388E3C),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "TEXT" -> {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Enviar Texto",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF57C00),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "LOCATION" -> {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Enviar Ubicación",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF616161),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "CALL" -> {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Llamada Automática",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1976D2).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .background(
                            color = Color(0xFFD32F2F).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
} 