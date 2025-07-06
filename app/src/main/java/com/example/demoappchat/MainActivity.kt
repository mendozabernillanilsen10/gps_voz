package com.example.demoappchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demoappchat.presentation.auth.AuthViewModel
import com.example.demoappchat.presentation.auth.LoginScreen
import com.example.demoappchat.presentation.chat.ChatScreen
import com.example.demoappchat.presentation.main.MainScreen
import com.example.demoappchat.ui.theme.SecurityChatTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.Composable
import com.google.firebase.FirebaseApp


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            SecurityChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecurityChatApp()
                }
            }
        }
    }
}

@Composable
fun SecurityChatApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

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