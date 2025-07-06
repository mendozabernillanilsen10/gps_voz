package com.example.demoappchat.presentation.components

// presentation/components/CreateChatDialog.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.data.model.ProximityChat
import com.example.demoappchat.ui.theme.EmergencyRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (ProximityChat) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("emergency") }

    val categories = listOf(
        "emergency" to "🚨 Emergencia",
        "security" to "🔒 Seguridad",
        "traffic" to "🚗 Tráfico",
        "community" to "🏘️ Comunidad"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = EmergencyRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear Chat de Seguridad",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Título del chat
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del chat") },
                    placeholder = { Text("Ej: Robo en Miraflores") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe la situación...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PIN
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("PIN (4 dígitos)") },
                    placeholder = { Text("1234") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Categoría
                Text(
                    text = "Categoría",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                categories.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank() && pin.length == 4) {
                        val chat = ProximityChat(
                            title = title,
                            description = description,
                            pin = pin,
                            category = selectedCategory
                        )
                        onCreateChat(chat)
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
            ) {
                Text("Crear Chat", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}