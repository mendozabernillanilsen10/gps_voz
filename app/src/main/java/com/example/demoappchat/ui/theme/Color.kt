package com.example.demoappchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores de emergencia
val EmergencyRed = Color(0xFFD32F2F)
val EmergencyRedDark = Color(0xFFB71C1C)
val EmergencyRedLight = Color(0xFFEF5350)

// Colores de seguridad
val SafetyGreen = Color(0xFF34C759)
val SafetyGreenDark = Color(0xFF2E7D32)
val SafetyGreenLight = Color(0xFF66BB6A)

// Colores de advertencia
val WarningOrange = Color(0xFFFF9500)
val WarningOrangeDark = Color(0xFFE65100)
val WarningOrangeLight = Color(0xFFFFB74D)

// Colores del sistema
val SystemRed = Color(0xFFFF3B30)
val SystemBlue = Color(0xFF007AFF)
val SystemBlueDark = Color(0xFF0056CC)
val SystemBlueLight = Color(0xFF5AC8FA)

// Colores de Facebook/WhatsApp para consistencia
val FacebookBlue = Color(0xFF1877F2)
val WhatsAppGreen = Color(0xFF42C85F)

// Grises para modo claro
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray600 = Color(0xFF757575)
val Gray700 = Color(0xFF616161)
val Gray800 = Color(0xFF424242)
val Gray900 = Color(0xFF212121)

// Grises para modo oscuro
val DarkGray50 = Color(0xFF1A1A1A)
val DarkGray100 = Color(0xFF2D2D2D)
val DarkGray200 = Color(0xFF404040)
val DarkGray300 = Color(0xFF525252)
val DarkGray400 = Color(0xFF656565)
val DarkGray500 = Color(0xFF787878)
val DarkGray600 = Color(0xFF8B8B8B)
val DarkGray700 = Color(0xFF9E9E9E)
val DarkGray800 = Color(0xFFB1B1B1)
val DarkGray900 = Color(0xFFC4C4C4)

// Colores de texto para modo claro
val TextPrimary = Color(0xFF1C1E21)
val TextSecondary = Color(0xFF65676B)
val TextTertiary = Color(0xFF8E8E93)

// Colores de texto para modo oscuro
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFB1B1B1)
val TextTertiaryDark = Color(0xFF8E8E93)

// Colores de fondo para modo claro
val BackgroundPrimary = Color(0xFFFFFFFF)
val BackgroundSecondary = Color(0xFFF2F2F7)
val BackgroundTertiary = Color(0xFFE5E5EA)

// Colores de fondo para modo oscuro
val BackgroundPrimaryDark = Color(0xFF000000)
val BackgroundSecondaryDark = Color(0xFF1C1C1E)
val BackgroundTertiaryDark = Color(0xFF2C2C2E)

// Colores de superficie para modo claro
val SurfacePrimary = Color(0xFFFFFFFF)
val SurfaceSecondary = Color(0xFFF9F9F9)
val SurfaceTertiary = Color(0xFFF2F2F7)

// Colores de superficie para modo oscuro
val SurfacePrimaryDark = Color(0xFF1C1C1E)
val SurfaceSecondaryDark = Color(0xFF2C2C2E)
val SurfaceTertiaryDark = Color(0xFF3A3A3C)

// Colores de borde para modo claro
val BorderPrimary = Color(0xFFE5E7EB)
val BorderSecondary = Color(0xFFD1D5DB)

// Colores de borde para modo oscuro
val BorderPrimaryDark = Color(0xFF38383A)
val BorderSecondaryDark = Color(0xFF48484A)

// Colores de mensaje
val MessageBubbleOwn = Color(0xFF007AFF)
val MessageBubbleOther = Color(0xFFFFFFFF)
val MessageBubbleOtherDark = Color(0xFF2C2C2E)
val MessageBackground = Color(0xFFF2F2F7)
val MessageBackgroundDark = Color(0xFF1C1C1E)

// Colores informativos
val InfoBlue = Color(0xFF2563EB)
val InfoBlueLight = Color(0xFF60A5FA)
val InfoBlueDark = Color(0xFF1E40AF)

// Colores legacy (mantener para compatibilidad)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val SecondaryGray = Color(0xFF6B7280)
val LightGray = Color(0xFFF9FAFB)
val DarkGray = Color(0xFF1F2937)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val SpyBlue = Color(0xFF1E40AF)
val AlertOrange = Color(0xFFEA580C)
val SafeGreen = Color(0xFF059669)
val VoiceActivation = Color(0xFF7C3AED)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Border = Color(0xFFE5E7EB)
val PrimaryBlue = Color(0xFF007AFF)
val PrimaryBlueDark = Color(0xFF0056CC)
val SecondaryBlue = Color(0xFF5AC8FA)
val BluePrimary = Color(0xFF007AFF)
val BluePrimaryDark = Color(0xFF0056CC)

// Funciones de utilidad para colores adaptativos
@Composable
fun adaptiveBackground(): Color {
    return if (isSystemInDarkTheme()) BackgroundPrimaryDark else BackgroundPrimary
}

@Composable
fun adaptiveSurface(): Color {
    return if (isSystemInDarkTheme()) SurfacePrimaryDark else SurfacePrimary
}

@Composable
fun adaptiveSurfaceVariant(): Color {
    return if (isSystemInDarkTheme()) SurfaceSecondaryDark else SurfaceSecondary
}

@Composable
fun adaptiveTextPrimary(): Color {
    return if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimary
}

@Composable
fun adaptiveTextSecondary(): Color {
    return if (isSystemInDarkTheme()) TextSecondaryDark else TextSecondary
}

@Composable
fun adaptiveBorder(): Color {
    return if (isSystemInDarkTheme()) BorderPrimaryDark else BorderPrimary
}

@Composable
fun adaptiveMessageBubbleOther(): Color {
    return if (isSystemInDarkTheme()) MessageBubbleOtherDark else MessageBubbleOther
}

@Composable
fun adaptiveMessageBackground(): Color {
    return if (isSystemInDarkTheme()) MessageBackgroundDark else MessageBackground
}



