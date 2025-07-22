package com.example.demoappchat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demoappchat.data.VoiceServicePreferences
import com.example.demoappchat.data.service.VoiceRecognitionService
import com.example.demoappchat.presentation.auth.AuthViewModel
import com.example.demoappchat.presentation.auth.LoginScreen
import com.example.demoappchat.presentation.chat.ChatScreen
import com.example.demoappchat.presentation.main.MainScreen
import com.example.demoappchat.presentation.splash.ModernSplashScreen
import com.example.demoappchat.ui.theme.SecurityChatTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.FirebaseApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var voiceServicePreferences: VoiceServicePreferences
    private var permissionsGranted = false

    // Lanzador de permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            // Si los permisos fueron concedidos y el servicio estaba habilitado, iniciarlo
            if (voiceServicePreferences.isVoiceServiceEnabled) {
                startVoiceService()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase y preferencias
        FirebaseApp.initializeApp(this)
        voiceServicePreferences = VoiceServicePreferences(this)

        // Solicitar permisos necesarios
        requestVoicePermissions()

        setContent {
            SecurityChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeVoiceApp(
                        onVoiceServiceToggle = { enabled ->
                            handleVoiceServiceToggle(enabled)
                        },
                        isVoiceServiceEnabled = voiceServicePreferences.isVoiceServiceEnabled
                    )
                }
            }
        }
    }

    private fun requestVoicePermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        )

        // Verificar si todos los permisos están concedidos
        permissionsGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!permissionsGranted) {
            permissionLauncher.launch(permissions)
        }
    }

    private fun handleVoiceServiceToggle(enabled: Boolean) {
        if (!permissionsGranted) {
            requestVoicePermissions()
            return
        }

        voiceServicePreferences.isVoiceServiceEnabled = enabled

        if (enabled) {
            startVoiceService()
        } else {
            stopVoiceService()
        }
    }

    private fun startVoiceService() {
        try {
            val intent = Intent(this, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_START_LISTENING

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVoiceService() {
        try {
            val intent = Intent(this, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_STOP_LISTENING
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No detener el servicio aquí para que continúe en segundo plano
    }
}

@Composable
fun SafeVoiceApp(
    onVoiceServiceToggle: (Boolean) -> Unit = {},
    isVoiceServiceEnabled: Boolean = false
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        ModernSplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = if (currentUser != null) "main" else "login"
        ) {
            composable("login") {
                LoginScreen(
                    onNavigateToMain = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainScreen(
                    onNavigateToChat = { chatId ->
                        navController.navigate("chat/$chatId")
                    },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    onVoiceServiceToggle = onVoiceServiceToggle,
                    isVoiceServiceEnabled = isVoiceServiceEnabled
                )
            }

            composable("chat/{chatId}") { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                ChatScreen(
                    chatId = chatId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}