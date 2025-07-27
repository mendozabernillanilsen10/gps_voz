package com.example.demoappchat.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.presentation.chat.VideoCallActivity

@Composable
fun VideoCallButton(
    chatId: String,
    participantName: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    
    Surface(
        onClick = {
            if (enabled) {
                startVideoCall(context, chatId, participantName)
            }
        },
        shape = CircleShape,
        color = if (enabled) Color(0xFF1877F2) else Color.Gray,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = "Video llamada",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VideoCallNotification(
    callerName: String,
    chatId: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Caller avatar
            Surface(
                shape = CircleShape,
                color = Color(0xFF1877F2),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = callerName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "Video llamada entrante",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Decline
                Surface(
                    onClick = onDecline,
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Rechazar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Accept
                Surface(
                    onClick = onAccept,
                    shape = CircleShape,
                    color = Color(0xFF42C85F),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Aceptar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatVideoCallHeader(
    participantName: String,
    onStartCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF0F2F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF1877F2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Video llamadas disponibles",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B)
                )
            }
            
            VideoCallButton(
                chatId = "", // Will be passed from parent
                participantName = participantName,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun startVideoCall(context: Context, chatId: String, participantName: String) {
    val intent = Intent(context, VideoCallActivity::class.java).apply {
        putExtra(VideoCallActivity.EXTRA_CHAT_ID, chatId)
        putExtra(VideoCallActivity.EXTRA_PARTICIPANT_NAME, participantName)
        putExtra(VideoCallActivity.EXTRA_IS_INCOMING_CALL, false)
    }
    context.startActivity(intent)
}

fun startIncomingVideoCall(context: Context, chatId: String, participantName: String) {
    val intent = Intent(context, VideoCallActivity::class.java).apply {
        putExtra(VideoCallActivity.EXTRA_CHAT_ID, chatId)
        putExtra(VideoCallActivity.EXTRA_PARTICIPANT_NAME, participantName)
        putExtra(VideoCallActivity.EXTRA_IS_INCOMING_CALL, true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    context.startActivity(intent)
}