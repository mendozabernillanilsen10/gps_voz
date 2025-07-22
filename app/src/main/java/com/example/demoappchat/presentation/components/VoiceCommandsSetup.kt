package com.example.demoappchat.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceCommandsSetup(
    onCommandsConfigured: (List<String>) -> Unit
) {
    var customCommands by remember { mutableStateOf(listOf("óyeme", "saa", "alerta")) }
    var newCommand by remember { mutableStateOf("") }
    var isServiceEnabled by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activación por Voz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = { enabled ->
                        isServiceEnabled = enabled
                        if (enabled) {
                            // Iniciar servicio
                            onCommandsConfigured(customCommands)
                        } else {
                            // Detener servicio
                            onCommandsConfigured(emptyList())
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Comandos de Activación:",
                fontWeight = FontWeight.Medium
            )

            customCommands.forEach { command ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\"$command\"")
                    IconButton(
                        onClick = {
                            customCommands = customCommands.filter { it != command }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar"
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommand,
                    onValueChange = { newCommand = it },
                    placeholder = { Text("Agregar comando...") },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (newCommand.isNotBlank()) {
                            customCommands = customCommands + newCommand
                            newCommand = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar"
                    )
                }
            }
        }
    }
}