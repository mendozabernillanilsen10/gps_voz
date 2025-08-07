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

private val DarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = Color.White,
    
    secondary = SafetyGreen,
    onSecondary = Color.White,
    secondaryContainer = SafetyGreenDark,
    onSecondaryContainer = Color.White,
    
    tertiary = WarningOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarningOrangeDark,
    onTertiaryContainer = Color.White,
    
    background = BackgroundPrimaryDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurfaceVariant = TextSecondaryDark,
    
    error = SystemRed,
    onError = Color.White,
    errorContainer = SystemRed.copy(alpha = 0.2f),
    onErrorContainer = Color.White,
    
    outline = BorderPrimaryDark,
    outlineVariant = BorderSecondaryDark,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = EmergencyRed.copy(alpha = 0.05f)
)

private val LightColorScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = EmergencyRedLight,
    onPrimaryContainer = Color.White,
    
    secondary = SafetyGreen,
    onSecondary = Color.White,
    secondaryContainer = SafetyGreenLight,
    onSecondaryContainer = Color.White,
    
    tertiary = WarningOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarningOrangeLight,
    onTertiaryContainer = Color.White,
    
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    
    error = SystemRed,
    onError = Color.White,
    errorContainer = SystemRed.copy(alpha = 0.1f),
    onErrorContainer = Color.White,
    
    outline = BorderPrimary,
    outlineVariant = BorderSecondary,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = EmergencyRed.copy(alpha = 0.05f)
)

@Composable
fun SecurityChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado para mantener consistencia
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
            // Configurar color de la barra de estado
            window.statusBarColor = if (darkTheme) {
                BackgroundPrimaryDark.toArgb()
            } else {
                EmergencyRed.toArgb()
            }
            // Configurar iconos de la barra de estado
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}