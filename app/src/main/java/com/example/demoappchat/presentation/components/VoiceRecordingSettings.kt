package com.example.demoappchat.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.ui.theme.*

/**
 * Componente para configurar las preferencias de grabación de voz
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingSettings(
    audioDuration: Int,
    videoDuration: Int,
    photoCaptureEnabled: Boolean,
    autoSendEnabled: Boolean,
    recordingQuality: String,
    commandActions: Map<String, String>,
    onAudioDurationChange: (Int) -> Unit,
    onVideoDurationChange: (Int) -> Unit,
    onPhotoCaptureToggle: (Boolean) -> Unit,
    onAutoSendToggle: (Boolean) -> Unit,
    onQualityChange: (String) -> Unit,
    onCommandActionChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showActionSelector by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Título
        Text(
            text = "Configuración de Grabación",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900
        )
        
        // Duración de grabación de audio
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Duración de Audio",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Configura cuánto tiempo grabar audio cuando se detecte un comando",
                    fontSize = 14.sp,
                    color = Gray600
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${audioDuration}s",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    
                    Slider(
                        value = audioDuration.toFloat(),
                        onValueChange = { onAudioDurationChange(it.toInt()) },
                        valueRange = 5f..300f,
                        steps = 58, // (300-5)/5 = 59 steps
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5s", fontSize = 12.sp, color = Gray500)
                    Text("300s", fontSize = 12.sp, color = Gray500)
                }
            }
        }
        
        // Duración de grabación de video
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Duración de Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Configura cuánto tiempo grabar video cuando se detecte un comando",
                    fontSize = 14.sp,
                    color = Gray600
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${videoDuration}s",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    
                    Slider(
                        value = videoDuration.toFloat(),
                        onValueChange = { onVideoDurationChange(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10, // (60-5)/5 = 11 steps
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5s", fontSize = 12.sp, color = Gray500)
                    Text("60s", fontSize = 12.sp, color = Gray500)
                }
            }
        }
        
        // Calidad de grabación
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = SafetyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Calidad de Grabación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                val qualities = listOf("LOW", "MEDIUM", "HIGH", "ULTRA")
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = recordingQuality == quality,
                            onClick = { onQualityChange(quality) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (quality) {
                                "LOW" -> "Baja (Ahorra espacio)"
                                "MEDIUM" -> "Media (Equilibrio)"
                                "HIGH" -> "Alta (Recomendada)"
                                "ULTRA" -> "Ultra (Máxima calidad)"
                                else -> quality
                            },
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // Opciones de envío
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Opciones de Envío",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Envío Automático",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enviar automáticamente al grupo",
                            fontSize = 12.sp,
                            color = Gray600
                        )
                    }
                    Switch(
                        checked = autoSendEnabled,
                        onCheckedChange = onAutoSendToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafetyGreen,
                            checkedTrackColor = SafetyGreen.copy(alpha = 0.3f)
                        )
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Captura de Fotos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Permitir comandos de foto",
                            fontSize = 12.sp,
                            color = Gray600
                        )
                    }
                    Switch(
                        checked = photoCaptureEnabled,
                        onCheckedChange = onPhotoCaptureToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafetyGreen,
                            checkedTrackColor = SafetyGreen.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        // Botón para ir a configuración de comandos
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Comandos de Voz Personalizados",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Configura comandos personalizados y sus acciones",
                    fontSize = 14.sp,
                    color = Gray600
                )
                
                Button(
                    onClick = { /* TODO: Navegar a pantalla de comandos */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Configurar Comandos")
                }
            }
        }
    }
} 