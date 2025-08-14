package com.example.demoappchat.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import com.example.demoappchat.presentation.settings.VoiceCommandUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommandDialogIntegrated(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var command by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("AUDIO") }
    var showActionSelector by remember { mutableStateOf(false) }
    
    val actions = listOf(
        "AUDIO" to "Grabar audio",
        "VIDEO" to "Grabar video", 
        "PHOTO" to "Capturar foto",
        "TEXT" to "Enviar mensaje",
        "LOCATION" to "Compartir ubicación",
        "CALL" to "Realizar llamada",
        "SOS" to "Activar emergencia",
        "TRACKING" to "Iniciar seguimiento",
        "SURVEILLANCE" to "Activar vigilancia",
        "STATUS" to "Reportar estado",
        "AUDIO_MESSAGE" to "Mensaje de audio",
        "VIDEO_MESSAGE" to "Mensaje de video",
        "PHOTO_MESSAGE" to "Mensaje con foto",
        "AUDIO_RECORDING" to "Grabación de audio",
        "STEALTH" to "Modo sigiloso"
    )
    
    val suggestedCommands = listOf(
        "grabar", "capturar", "filmar", "foto", "video", "audio",
        "emergencia", "ayuda", "sos", "alerta", "vigilancia",
        "ubicación", "posición", "mensaje", "texto", "llamar"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Agregar Comando de Voz",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo de comando
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Comando de voz",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                OutlinedTextField(
                    value = command,
                        onValueChange = { command = it.lowercase() },
                        placeholder = {
                            Text("Ej: 'grabar audio', 'emergencia', 'foto'")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                }
                
                // Comandos sugeridos
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        text = "Comandos sugeridos",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        items(suggestedCommands) { suggested ->
                            SuggestionChip(
                                onClick = { command = suggested },
                                label = { Text(suggested) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
                
                // Selector de acción
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                        text = "Acción a ejecutar",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActionSelector = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    VoiceCommandUtils.getActionIcon(selectedAction),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = VoiceCommandUtils.getActionDescription(selectedAction),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toca para cambiar",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Seleccionar acción",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Información adicional
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "La app escuchará continuamente este comando y ejecutará la acción automáticamente",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (command.isNotBlank()) {
                        onConfirm(command.trim(), selectedAction)
                    }
                },
                enabled = command.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar Comando")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
    
    // Dialog para seleccionar acción
    if (showActionSelector) {
        AlertDialog(
            onDismissRequest = { showActionSelector = false },
            title = {
                Text(
                    "Seleccionar Acción",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(actions) { (action, description) ->
                        ActionOption(
                            action = action,
                            description = description,
                            isSelected = selectedAction == action,
                            onClick = {
                                selectedAction = action
                                showActionSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionSelector = false }) {
                Text("Cancelar")
            }
        }
    )
    }
}

@Composable
fun EditCommandDialogIntegrated(
    command: String,
    currentAction: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedAction by remember { mutableStateOf(currentAction) }
    var showActionSelector by remember { mutableStateOf(false) }
    
    val actions = listOf(
        "AUDIO" to "Grabar audio",
        "VIDEO" to "Grabar video", 
        "PHOTO" to "Capturar foto",
        "TEXT" to "Enviar mensaje",
        "LOCATION" to "Compartir ubicación",
        "CALL" to "Realizar llamada",
        "SOS" to "Activar emergencia",
        "TRACKING" to "Iniciar seguimiento",
        "SURVEILLANCE" to "Activar vigilancia",
        "STATUS" to "Reportar estado",
        "AUDIO_MESSAGE" to "Mensaje de audio",
        "VIDEO_MESSAGE" to "Mensaje de video",
        "PHOTO_MESSAGE" to "Mensaje con foto",
        "AUDIO_RECORDING" to "Grabación de audio",
        "STEALTH" to "Modo sigiloso"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Editar Comando",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mostrar comando actual
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Comando actual",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                Text(
                                text = "\"$command\"",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                )
                        }
                    }
                }
                
                // Selector de acción
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        text = "Cambiar acción",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActionSelector = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    VoiceCommandUtils.getActionIcon(selectedAction),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = VoiceCommandUtils.getActionDescription(selectedAction),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toca para cambiar",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Seleccionar acción",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Información de cambios
                if (selectedAction != currentAction) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Update,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cambio de '${VoiceCommandUtils.getActionDescription(currentAction)}' a '${VoiceCommandUtils.getActionDescription(selectedAction)}'",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
    
    // Dialog para seleccionar acción
    if (showActionSelector) {
        AlertDialog(
            onDismissRequest = { showActionSelector = false },
            title = {
                Text(
                    "Seleccionar Nueva Acción",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(actions) { (action, description) ->
                        ActionOption(
                            action = action,
                            description = description,
                            isSelected = selectedAction == action,
                            onClick = {
                                selectedAction = action
                                showActionSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionSelector = false }) {
                Text("Cancelar")
            }
        }
    )
    }
}

@Composable
fun ActionOption(
    action: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                VoiceCommandUtils.getActionIcon(action),
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = action,
                    fontSize = 12.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 