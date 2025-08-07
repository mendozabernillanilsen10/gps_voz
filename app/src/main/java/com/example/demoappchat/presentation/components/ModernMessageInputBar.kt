package com.example.demoappchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachmentClick: () -> Unit,
    isLoading: Boolean,
    onPhotoClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onDocumentClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    var showAttachmentOptions by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            // Main input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment button with modern design
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (showAttachmentOptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .clickable { showAttachmentOptions = !showAttachmentOptions },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (showAttachmentOptions) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Adjuntar",
                        tint = if (showAttachmentOptions) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Enhanced text field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Mensaje...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Send button with modern design
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .clickable {
                            if (messageText.isNotBlank()) {
                                onSendMessage()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Attachment options grid
            AnimatedVisibility(
                visible = showAttachmentOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.PhotoCamera,
                                label = "Foto",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onPhotoClick
                            )
                        }
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.Videocam,
                                label = "Video",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onVideoClick
                            )
                        }
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.Mic,
                                label = "Audio",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onAudioClick
                            )
                        }
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.LocationOn,
                                label = "Ubicación",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onLocationClick
                            )
                        }
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.Description,
                                label = "Documento",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onDocumentClick
                            )
                        }
                        item {
                            AttachmentOption(
                                icon = Icons.Rounded.Camera,
                                label = "Cámara",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onCameraClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModernMediaPickerDialog(
    onDismiss: () -> Unit,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLocationClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Seleccionar opción",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Options grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    item {
                        AttachmentOption(
                            icon = Icons.Default.PhotoCamera,
                            label = "Cámara",
                            color = Color(0xFF4CAF50),
                            onClick = {
                                onCameraClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Photo,
                            label = "Galería",
                            color = Color(0xFF2196F3),
                            onClick = {
                                onPhotoClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            color = Color(0xFFE91E63),
                            onClick = {
                                onVideoClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Mic,
                            label = "Audio",
                            color = Color(0xFF9C27B0),
                            onClick = {
                                onAudioClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        AttachmentOption(
                            icon = Icons.Default.LocationOn,
                            label = "Ubicación",
                            color = Color(0xFFFF9800),
                            onClick = {
                                onLocationClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        AttachmentOption(
                            icon = Icons.Default.Description,
                            label = "Documento",
                            color = Color(0xFF607D8B),
                            onClick = {
                                onDocumentClick()
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}