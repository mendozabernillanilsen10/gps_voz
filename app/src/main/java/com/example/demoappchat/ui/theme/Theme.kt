// ui/theme/Theme.kt
package com.example.demoappchat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===== ESQUEMA DE COLORES MINIMALISTA PARA MODO OSCURO =====
private val DarkColorScheme = darkColorScheme(
    primary = MinimalistBlue,
    onPrimary = Color.White,
    primaryContainer = MinimalistBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = MinimalistGreen,
    onSecondary = Color.White,
    secondaryContainer = MinimalistGreenDark,
    onSecondaryContainer = Color.White,
    
    tertiary = MinimalistPurple,
    onTertiary = Color.White,
    tertiaryContainer = MinimalistPurpleDark,
    onTertiaryContainer = Color.White,
    
    background = BackgroundPrimaryDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurfaceVariant = TextSecondaryDark,
    
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = Color.White,
    
    outline = BorderPrimaryDark,
    outlineVariant = BorderSecondaryDark,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = MinimalistBlue.copy(alpha = 0.05f)
)

// ===== ESQUEMA DE COLORES MINIMALISTA PARA MODO CLARO =====
private val LightColorScheme = lightColorScheme(
    primary = MinimalistBlue,
    onPrimary = Color.White,
    primaryContainer = MinimalistBlueLight,
    onPrimaryContainer = MinimalistBlueDark,
    
    secondary = MinimalistGreen,
    onSecondary = Color.White,
    secondaryContainer = MinimalistGreenLight,
    onSecondaryContainer = MinimalistGreenDark,
    
    tertiary = MinimalistPurple,
    onTertiary = Color.White,
    tertiaryContainer = MinimalistPurpleLight,
    onTertiaryContainer = MinimalistPurpleDark,
    
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error,
    
    outline = BorderPrimary,
    outlineVariant = BorderSecondary,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = MinimalistBlue.copy(alpha = 0.05f)
)

@Composable
fun SecurityChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado para mantener consistencia minimalista
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Configuración de la barra de estado para modo oscuro/claro
            if (darkTheme) {
                // Modo oscuro: barra de estado clara
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            } else {
                // Modo claro: barra de estado oscura
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
            
            // Configuración de la barra de navegación
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}