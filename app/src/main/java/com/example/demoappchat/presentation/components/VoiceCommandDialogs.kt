package com.example.demoappchat.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommandDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var command by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("AUDIO") }
    
    val actions = listOf("AUDIO", "VIDEO", "PHOTO", "TEXT", "LOCATION", "CALL")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar Comando",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Comando de voz") },
                    placeholder = { Text("Ej: grabar audio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Seleccionar acción:",
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = actions,
                        key = { it }
                    ) { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            label = {
                                Text(
                                    text = when (action) {
                                        "AUDIO" -> "Grabar Audio"
                                        "VIDEO" -> "Grabar Video"
                                        "PHOTO" -> "Tomar Foto"
                                        "TEXT" -> "Enviar Texto"
                                        "LOCATION" -> "Enviar Ubicación"
                                        "CALL" -> "Llamada Automática"
                                        else -> action
                                    }
                                )
                            },
                            leadingIcon = {
                                when (action) {
                                    "AUDIO" -> Icon(Icons.Default.Mic, null)
                                    "VIDEO" -> Icon(Icons.Default.Videocam, null)
                                    "PHOTO" -> Icon(Icons.Default.PhotoCamera, null)
                                    "TEXT" -> Icon(Icons.Default.Chat, null)
                                    "LOCATION" -> Icon(Icons.Default.LocationOn, null)
                                    "CALL" -> Icon(Icons.Default.Call, null)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(command, selectedAction) },
                enabled = command.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommandDialog(
    command: String,
    currentAction: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedAction by remember { mutableStateOf(currentAction) }
    
    val actions = listOf("AUDIO", "VIDEO", "PHOTO", "TEXT", "LOCATION", "CALL")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Comando",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Comando: \"$command\"",
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Seleccionar nueva acción:",
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = actions,
                        key = { it }
                    ) { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            label = {
                                Text(
                                    text = when (action) {
                                        "AUDIO" -> "Grabar Audio"
                                        "VIDEO" -> "Grabar Video"
                                        "PHOTO" -> "Tomar Foto"
                                        "TEXT" -> "Enviar Texto"
                                        "LOCATION" -> "Enviar Ubicación"
                                        "CALL" -> "Llamada Automática"
                                        else -> action
                                    }
                                )
                            },
                            leadingIcon = {
                                when (action) {
                                    "AUDIO" -> Icon(Icons.Default.Mic, null)
                                    "VIDEO" -> Icon(Icons.Default.Videocam, null)
                                    "PHOTO" -> Icon(Icons.Default.PhotoCamera, null)
                                    "TEXT" -> Icon(Icons.Default.Chat, null)
                                    "LOCATION" -> Icon(Icons.Default.LocationOn, null)
                                    "CALL" -> Icon(Icons.Default.Call, null)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAction) }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 