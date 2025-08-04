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
import com.example.demoappchat.ui.theme.*
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
        color = Color.White,
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
                            if (showAttachmentOptions) PrimaryBlue else Gray100,
                            CircleShape
                        )
                        .clickable { showAttachmentOptions = !showAttachmentOptions },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (showAttachmentOptions) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Adjuntar",
                        tint = if (showAttachmentOptions) Color.White else Gray600,
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
                            color = Gray500,
                            fontSize = 16.sp
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                        unfocusedBorderColor = Gray300,
                        focusedContainerColor = Gray50,
                        unfocusedContainerColor = Gray50
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Enhanced send button
                val sendButtonScale by animateFloatAsState(
                    targetValue = if (messageText.isNotBlank()) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "send_button_scale"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(sendButtonScale)
                        .clip(CircleShape)
                        .background(
                            if (messageText.isNotBlank()) PrimaryBlue else Gray300,
                            CircleShape
                        )
                        .clickable(
                            enabled = messageText.isNotBlank() && !isLoading
                        ) { onSendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Attachment options panel (Instagram style)
            AnimatedVisibility(
                visible = showAttachmentOptions,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                AttachmentOptionsPanel(
                    onPhotoClick = {
                        onPhotoClick()
                        showAttachmentOptions = false
                    },
                    onVideoClick = {
                        onVideoClick()
                        showAttachmentOptions = false
                    },
                    onAudioClick = {
                        onAudioClick()
                        showAttachmentOptions = false
                    },
                    onLocationClick = {
                        onLocationClick()
                        showAttachmentOptions = false
                    },
                    onDocumentClick = {
                        onDocumentClick()
                        showAttachmentOptions = false
                    },
                    onCameraClick = {
                        onCameraClick()
                        showAttachmentOptions = false
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentOptionsPanel(
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLocationClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = Gray50,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Adjuntar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.PhotoCamera,
                    label = "Cámara",
                    color = Color(0xFF4CAF50),
                    onClick = onCameraClick
                )
                
                AttachmentOption(
                    icon = Icons.Default.Photo,
                    label = "Galería",
                    color = Color(0xFF2196F3),
                    onClick = onPhotoClick
                )
                
                AttachmentOption(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    color = Color(0xFFE91E63),
                    onClick = onVideoClick
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.Mic,
                    label = "Audio",
                    color = Color(0xFF9C27B0),
                    onClick = onAudioClick
                )
                
                AttachmentOption(
                    icon = Icons.Default.LocationOn,
                    label = "Ubicación",
                    color = Color(0xFFFF9800),
                    onClick = onLocationClick
                )
                
                AttachmentOption(
                    icon = Icons.Default.Description,
                    label = "Documento",
                    color = Color(0xFF607D8B),
                    onClick = onDocumentClick
                )
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
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Gray700,
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
            color = Color.White,
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
                    color = Gray900,
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
                        color = Gray600,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}