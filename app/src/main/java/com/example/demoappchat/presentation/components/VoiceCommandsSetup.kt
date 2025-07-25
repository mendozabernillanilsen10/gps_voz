package com.example.demoappchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
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
import com.example.demoappchat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandsSetup(
    onCommandsConfigured: (List<String>) -> Unit,
    isServiceEnabled: Boolean = false,
    isRecording: Boolean = false,
    recordingType: String = "",
    onServiceToggle: (Boolean) -> Unit = {},
    onDiscreteModeToggle: (Boolean) -> Unit = {},
    discreteMode: Boolean = true,
    onRadiusChange: (Int) -> Unit = {}
) {
    var customCommands by remember {
        mutableStateOf(listOf("óyeme", "saa", "alerta", "grabar audio", "grabar video"))
    }
    var newCommand by remember { mutableStateOf("") }
    var emergencyRadius by remember { mutableStateOf(300) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header con estado del servicio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                Text(
                    text = "Activación por Voz",
                    fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Gray900
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    when {
                        isRecording -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SystemRed, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Grabando: $recordingType",
                                    color = SystemRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        isServiceEnabled -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SafetyGreen, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Servicio activo",
                                    color = SafetyGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Gray400, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Servicio inactivo",
                                    color = Gray500,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = { enabled ->
                        onServiceToggle(enabled)
                        if (enabled) {
                            onCommandsConfigured(customCommands)
                        } else {
                            onCommandsConfigured(emptyList())
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Gray300
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Modo discreto
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Gray50
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Modo Discreto",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Gray900
                            )
            Text(
                                text = "Sin notificaciones visibles",
                                fontSize = 12.sp,
                                color = Gray600
                            )
                        }
                    }
                    Switch(
                        checked = discreteMode,
                        onCheckedChange = { onDiscreteModeToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Gray300
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radio de emergencia
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Radio de Emergencia",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Gray900
                    )
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${emergencyRadius}m",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = emergencyRadius.toFloat(),
                    onValueChange = {
                        emergencyRadius = it.toInt()
                        onRadiusChange(emergencyRadius)
                    },
                    valueRange = 100f..1000f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = Gray300
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "100m",
                        fontSize = 12.sp,
                        color = Gray500
                    )
                    Text(
                        text = "1000m",
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Comandos de activación
            Text(
                text = "Comandos de Activación",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Gray900
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comandos predefinidos
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = customCommands.contains("óyeme"),
                        onClick = {
                            customCommands = if (customCommands.contains("óyeme")) {
                                customCommands.filter { it != "óyeme" }
                            } else {
                                customCommands + "óyeme"
                            }
                        },
                        label = { Text("óyeme") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = customCommands.contains("alerta"),
                        onClick = {
                            customCommands = if (customCommands.contains("alerta")) {
                                customCommands.filter { it != "alerta" }
                            } else {
                                customCommands + "alerta"
                            }
                        },
                        label = { Text("alerta") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = customCommands.contains("grabar audio"),
                        onClick = {
                            customCommands = if (customCommands.contains("grabar audio")) {
                                customCommands.filter { it != "grabar audio" }
                            } else {
                                customCommands + "grabar audio"
                            }
                        },
                        label = { Text("grabar audio") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafetyGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = customCommands.contains("grabar video"),
                        onClick = {
                            customCommands = if (customCommands.contains("grabar video")) {
                                customCommands.filter { it != "grabar video" }
                            } else {
                                customCommands + "grabar video"
                            }
                        },
                        label = { Text("grabar video") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SystemRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comandos personalizados
            val personalizedCommands = customCommands.filter {
                it !in listOf("óyeme", "alerta", "grabar audio", "grabar video")
            }

            if (personalizedCommands.isNotEmpty()) {
                Text(
                    text = "Comandos Personalizados",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Gray700
                )

                Spacer(modifier = Modifier.height(8.dp))

                personalizedCommands.forEach { command ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Gray50
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                        onClick = {
                            customCommands = customCommands.filter { it != command }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = SystemRed,
                                    modifier = Modifier.size(20.dp)
                        )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Agregar nuevo comando
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommand,
                    onValueChange = { newCommand = it },
                    placeholder = { Text("Agregar comando personalizado...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        cursorColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newCommand.isNotBlank() && !customCommands.contains(newCommand)) {
                            customCommands = customCommands + newCommand
                            newCommand = ""
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (newCommand.isNotBlank()) PrimaryBlue else Gray300,
                            androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Información de uso
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cómo usar los comandos de voz",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row {
                            Text(
                                text = "• ",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                            Text(
                                text = "Di \"óyeme\" o \"alerta\" para activar grabación de audio",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                        }
                        Row {
                            Text(
                                text = "• ",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                            Text(
                                text = "Di \"grabar video\" para activar grabación de video",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                        }
                        Row {
                            Text(
                                text = "• ",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                            Text(
                                text = "Las grabaciones se envían automáticamente al chat",
                                fontSize = 13.sp,
                                color = Gray700
                            )
                        }
                    }
                }
            }
        }
    }
}