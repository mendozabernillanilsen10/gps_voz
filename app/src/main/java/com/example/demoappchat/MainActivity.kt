package com.example.demoappchat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import com.example.demoappchat.data.UserPreferences
import com.example.demoappchat.data.service.VoiceRecognitionService
import com.example.demoappchat.presentation.auth.AuthViewModel
import com.example.demoappchat.presentation.auth.LoginScreen
import com.example.demoappchat.presentation.chat.ChatScreen
import com.example.demoappchat.presentation.main.MainScreen
import com.example.demoappchat.presentation.settings.SettingsScreen
import com.example.demoappchat.presentation.splash.ModernSplashScreen
import com.example.demoappchat.ui.theme.SecurityChatTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.FirebaseApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var userPreferences: UserPreferences
    private var permissionsGranted = false

    // Lanzador de permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            // Si los permisos fueron concedidos y el servicio estaba habilitado, iniciarlo
            // Voice service will be handled through the new settings system
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase y preferencias
        FirebaseApp.initializeApp(this)
        // Initialize userPreferences through Hilt injection

        // Solicitar permisos necesarios
        requestVoicePermissions()
        
        // Solicitar exención de optimización de batería
        requestBatteryOptimizationExemption()

        setContent {
            SecurityChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeVoiceApp()
                }
            }
        }
    }

    private fun requestVoicePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WAKE_LOCK
        )
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Add storage permissions based on API level
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Verificar si todos los permisos están concedidos
        permissionsGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!permissionsGranted) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Si no se puede abrir la configuración específica, abrir la general
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    private fun handleVoiceServiceToggle(enabled: Boolean) {
        if (!permissionsGranted) {
            requestVoicePermissions()
            return
        }

        // Voice service preferences now handled through UserPreferences in Settings

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
fun SafeVoiceApp() {
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
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
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