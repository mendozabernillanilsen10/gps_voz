package com.example.demoappchat.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.demoappchat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachmentClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment button
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Gray100, CircleShape)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Adjuntar",
                    tint = Gray600,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text field
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Mensaje...",
                        color = Gray500
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = Gray300,
                    focusedContainerColor = Gray50,
                    unfocusedContainerColor = Gray50
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            val sendButtonScale by animateFloatAsState(
                targetValue = if (messageText.isNotBlank()) 1f else 0.9f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "send_button_scale"
            )

            IconButton(
                onClick = onSendMessage,
                enabled = messageText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .scale(sendButtonScale)
                    .background(
                        if (messageText.isNotBlank()) PrimaryBlue else Gray300,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}