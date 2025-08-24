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
import com.example.demoappchat.data.service.BackgroundVoiceService
import com.example.demoappchat.data.receiver.VoiceCommandReceiver
import com.example.demoappchat.presentation.auth.AuthViewModel
import com.example.demoappchat.presentation.auth.LoginScreen
import com.example.demoappchat.presentation.chat.ChatScreen
import com.example.demoappchat.presentation.main.MainScreen
import com.example.demoappchat.presentation.settings.SettingsScreen
import com.example.demoappchat.presentation.splash.ModernSplashScreen
import com.example.demoappchat.ui.theme.SecurityChatTheme
import com.example.demoappchat.utils.NavigationHelper
import com.example.demoappchat.data.service.ErrorLogger
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.FirebaseApp
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val errorLogger = ErrorLogger()
    
    private lateinit var userPreferences: UserPreferences
    private var permissionsGranted = false

    // Lanzador de permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            // Inicializar todos los servicios de voz automáticamente
            initializeAllVoiceServices()
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

        // Registrar receptor de comandos de voz
        registerVoiceCommandReceiver()

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

    /**
     * Inicializa todos los servicios de voz automáticamente
     */
    private fun initializeAllVoiceServices() {
        try {
            Log.d("MainActivity", "🚀 Inicializando todos los servicios de voz...")
            
            // 1. Iniciar servicio de reconocimiento de voz principal
            startVoiceRecognitionService()
            
            // 2. Iniciar servicio de voz en segundo plano
            startBackgroundVoiceService()
            
            // 3. Configurar comandos de voz automáticos
            setupAutomaticVoiceCommands()
            
            // 4. Activar modo 24/7
            activate24x7Mode()
            
            Log.d("MainActivity", "✅ Todos los servicios de voz inicializados correctamente")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error inicializando servicios de voz", e)
        }
    }

    /**
     * Inicia el servicio de reconocimiento de voz principal
     */
    private fun startVoiceRecognitionService() {
        try {
            Log.d("MainActivity", "🎤 Iniciando servicio de reconocimiento de voz...")
            
            val intent = Intent(this, VoiceRecognitionService::class.java)
            intent.action = VoiceRecognitionService.ACTION_START_LISTENING

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            Log.d("MainActivity", "✅ Servicio de reconocimiento de voz iniciado")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error iniciando servicio de reconocimiento", e)
        }
    }

    /**
     * Inicia el servicio de voz en segundo plano
     */
    private fun startBackgroundVoiceService() {
        try {
            Log.d("MainActivity", "🎧 Iniciando servicio de voz en segundo plano...")
            
            val intent = Intent(this, BackgroundVoiceService::class.java)
            intent.action = BackgroundVoiceService.ACTION_START_BACKGROUND

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            Log.d("MainActivity", "✅ Servicio de voz en segundo plano iniciado")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error iniciando servicio de fondo", e)
        }
    }

    /**
     * Configura comandos de voz automáticos para crear chats grupales
     */
    private fun setupAutomaticVoiceCommands() {
        try {
            Log.d("MainActivity", "🎯 Configurando comandos de voz automáticos...")
            
            // Configurar comandos que crean chats grupales automáticamente
            val automaticCommands = mapOf(
                "emergencia" to "CREATE_EMERGENCY_CHAT",
                "ayuda" to "CREATE_EMERGENCY_CHAT", 
                "socorro" to "CREATE_EMERGENCY_CHAT",
                "alerta" to "CREATE_ALERT_CHAT",
                "vigilancia" to "CREATE_SURVEILLANCE_CHAT",
                "observar" to "CREATE_SURVEILLANCE_CHAT",
                "monitorear" to "CREATE_SURVEILLANCE_CHAT",
                "grabar" to "CREATE_RECORDING_CHAT",
                "audio" to "CREATE_RECORDING_CHAT",
                "sonido" to "CREATE_RECORDING_CHAT",
                "chat grupal" to "CREATE_GENERAL_CHAT",
                "grupo" to "CREATE_GENERAL_CHAT",
                "conversar" to "CREATE_GENERAL_CHAT"
            )
            
            // Guardar comandos en SharedPreferences
            val sharedPrefs = getSharedPreferences("voice_prefs", MODE_PRIVATE)
            val commandsString = automaticCommands.map { "${it.key}:${it.value}" }.joinToString(",")
            sharedPrefs.edit().putString("automatic_commands", commandsString).apply()
            
            Log.d("MainActivity", "✅ Comandos automáticos configurados: $commandsString")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error configurando comandos automáticos", e)
        }
    }

    /**
     * Activa el modo 24/7 para funcionar siempre
     */
    private fun activate24x7Mode() {
        try {
            Log.d("MainActivity", "🔄 Activando modo 24/7...")
            
            // Configurar para que los servicios se reinicien automáticamente
            val sharedPrefs = getSharedPreferences("voice_prefs", MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("24x7_mode", true)
                .putBoolean("auto_restart", true)
                .putLong("last_activation", System.currentTimeMillis())
                .apply()
            
            Log.d("MainActivity", "✅ Modo 24/7 activado")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error activando modo 24/7", e)
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
        } else {
            // Si los permisos ya están concedidos, inicializar servicios
            initializeAllVoiceServices()
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

    private fun registerVoiceCommandReceiver() {
        try {
            VoiceCommandReceiver.register(this)
            android.util.Log.d("MainActivity", "✅ Receptor de comandos de voz registrado")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error registrando receptor de comandos de voz", e)
        }
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
                        try {
                            android.util.Log.d("MainActivity", "🔄 Navegando a chat: $chatId")
                            
                            // Validar que el chatId no esté vacío
                            if (chatId.isNotBlank()) {
                                navController.navigate("chat/$chatId") {
                                    // Evitar múltiples instancias del mismo chat
                                    launchSingleTop = true
                                }
                                android.util.Log.d("MainActivity", "✅ Navegación exitosa a chat: $chatId")
                                
                                // Log éxito para Honor devices
                               // if (ErrorLogger.isHonorDevice()) {
                                    ErrorLogger().logHonorSpecificIssue(
                                        issue = "navigation_success",
                                        context = "MainActivity.onNavigateToChat",
                                        additionalData = mapOf(
                                            "chat_id" to chatId,
                                            "navigation_method" to "traditional"
                                        )
                                    )
                               // }
                            } else {
                                android.util.Log.e("MainActivity", "❌ ChatId vacío, no se puede navegar")
                                
                                ErrorLogger().logNavigationError(
                                    fromScreen = "main",
                                    toScreen = "chat",
                                    chatId = "empty",
                                    throwable = Exception("ChatId vacío"),
                                    additionalData = mapOf(
                                        "validation_error" to "empty_chat_id"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error en navegación a chat: $chatId", e)
                            
                            ErrorLogger().logNavigationError(
                                fromScreen = "main",
                                toScreen = "chat",
                                chatId = chatId,
                                throwable = e,
                                additionalData = mapOf(
                                    "navigation_method" to "traditional",
                                    "device_info" to ErrorLogger.getDeviceInfo()
                                )
                            )
                        }
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
                
                android.util.Log.d("MainActivity", "🎯 Cargando ChatScreen con ID: $chatId")
                
                if (chatId.isNotBlank()) {
                    ChatScreen(
                        chatId = chatId,
                        onNavigateBack = {
                            try {
                                android.util.Log.d("MainActivity", "⬅️ Volviendo desde chat: $chatId")
                                navController.popBackStack()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "❌ Error al volver desde chat", e)
                            }
                        }
                    )
                } else {
                    // Si no hay chatId válido, volver al main
                    android.util.Log.e("MainActivity", "❌ ChatId inválido, volviendo a main")
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}