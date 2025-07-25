package com.example.demoappchat.presentation.components
import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.demoappchat.ui.theme.EmergencyRed
import com.example.demoappchat.ui.theme.Gray600
import com.example.demoappchat.ui.theme.Gray700
import com.example.demoappchat.ui.theme.Gray900
import com.example.demoappchat.ui.theme.PrimaryBlue
import com.example.demoappchat.ui.theme.SafetyGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatTopBar(
    chatTitle: String,
    participantCount: Int,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar moderno
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryBlue.copy(alpha = 0.2f),
                                    PrimaryBlue.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chatTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900,
                        fontSize = 17.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SafetyGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$participantCount participantes",
                            color = Gray600,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Gray700,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        ),
        actions = {
            IconButton(onClick = { /* Video call */ }) {
                Icon(
                    Icons.Rounded.Videocam,
                    contentDescription = "Videollamada",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = { /* Voice call */ }) {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = "Llamada",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}