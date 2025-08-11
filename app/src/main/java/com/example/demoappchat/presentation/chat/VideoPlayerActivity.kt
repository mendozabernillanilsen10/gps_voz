package com.example.demoappchat.presentation.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier


class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar pantalla completa
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Ocultar barra de navegación
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                VideoPlayerFullScreen(
                    videoUrl = videoUrl,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Asegurar que la pantalla completa se mantenga
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
