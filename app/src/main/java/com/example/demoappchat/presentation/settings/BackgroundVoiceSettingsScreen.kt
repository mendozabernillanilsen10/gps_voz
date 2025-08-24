package com.example.demoappchat.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.demoappchat.domain.usecase.voice.StartBackgroundVoiceServiceUseCase
import com.example.demoappchat.domain.usecase.voice.StopBackgroundVoiceServiceUseCase

@Composable
fun BackgroundVoiceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackgroundVoiceSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
            
            Text(
                text = "🎤 Voz en Segundo Plano",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Estado del servicio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isServiceRunning) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isServiceRunning) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Cancel,
                        contentDescription = "Estado del servicio",
                        tint = if (uiState.isServiceRunning) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = if (uiState.isServiceRunning) 
                            "Servicio Activo" 
                        else 
                            "Servicio Inactivo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (uiState.isServiceRunning)
                        "La app está escuchando comandos de voz en segundo plano"
                    else
                        "El servicio de voz en segundo plano está desactivado",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controles del servicio
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Control del Servicio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startBackgroundService() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isServiceRunning && !uiState.isLoading
                    ) {
                        Text("Iniciar Servicio")
                    }
                    
                    Button(
                        onClick = { viewModel.stopBackgroundService() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isServiceRunning && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Detener Servicio")
                    }
                }
                
                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Configuración de comandos
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Comandos de Voz Disponibles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CommandItem(
                    command = "Emergencia",
                    description = "Crea chat de emergencia automáticamente",
                    keywords = listOf("emergencia", "ayuda", "socorro")
                )
                
                CommandItem(
                    command = "Vigilancia",
                    description = "Activa modo de vigilancia",
                    keywords = listOf("vigilancia", "observar", "monitorear")
                )
                
                CommandItem(
                    command = "Grabar",
                    description = "Inicia grabación de audio automática",
                    keywords = listOf("grabar", "audio", "sonido")
                )
                
                CommandItem(
                    command = "Chat",
                    description = "Crea chat grupal automáticamente",
                    keywords = listOf("chat", "grupo", "conversar")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Información del servicio
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Información del Servicio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoItem(
                    title = "Funcionamiento",
                    description = "El servicio funciona en segundo plano incluso cuando la app está cerrada"
                )
                
                InfoItem(
                    title = "Batería",
                    description = "Consume batería adicional para mantener el micrófono activo"
                )
                
                InfoItem(
                    title = "Privacidad",
                    description = "Solo escucha comandos específicos, no graba conversaciones"
                )
                
                InfoItem(
                    title = "Notificaciones",
                    description = "Envía notificaciones a otros usuarios cuando detecta comandos"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Configuración por defecto
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Configuración por Defecto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DefaultConfigItem(
                    title = "Chat de Emergencia",
                    config = "Radio: 5km, PIN: 1234"
                )
                
                DefaultConfigItem(
                    title = "Chat de Vigilancia",
                    config = "Radio: 3km, PIN: 5678"
                )
                
                DefaultConfigItem(
                    title = "Chat General",
                    config = "Radio: 4km, PIN: 9999"
                )
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: String,
    description: String,
    keywords: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = command,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Palabras clave: ${keywords.joinToString(", ")}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun InfoItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DefaultConfigItem(
    title: String,
    config: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = config,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
