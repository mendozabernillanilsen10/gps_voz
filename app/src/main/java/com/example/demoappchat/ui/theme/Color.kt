package com.example.demoappchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ===== PALETA MINIMALISTA PRINCIPAL =====
// Azules modernos y suaves
val MinimalistBlue = Color(0xFF3B82F6)      // Azul principal moderno
val MinimalistBlueDark = Color(0xFF1D4ED8)  // Azul oscuro
val MinimalistBlueLight = Color(0xFF60A5FA)  // Azul claro
val MinimalistBlueMuted = Color(0xFFDBEAFE) // Azul muy suave

// Verdes minimalistas
val MinimalistGreen = Color(0xFF10B981)     // Verde suave
val MinimalistGreenDark = Color(0xFF047857) // Verde oscuro
val MinimalistGreenLight = Color(0xFF34D399) // Verde claro
val MinimalistGreenMuted = Color(0xFFD1FAE5) // Verde muy suave

// Púrpuras modernos
val MinimalistPurple = Color(0xFF8B5CF6)    // Púrpura suave
val MinimalistPurpleDark = Color(0xFF6D28D9) // Púrpura oscuro
val MinimalistPurpleLight = Color(0xFFA78BFA) // Púrpura claro
val MinimalistPurpleMuted = Color(0xFFEDE9FE) // Púrpura muy suave

// ===== GRISES MINIMALISTAS PARA MODO CLARO =====
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

// ===== GRISES MINIMALISTAS PARA MODO OSCURO =====
val DarkGray50 = Color(0xFF0A0A0A)
val DarkGray100 = Color(0xFF141414)
val DarkGray200 = Color(0xFF1E1E1E)
val DarkGray300 = Color(0xFF2D2D2D)
val DarkGray400 = Color(0xFF404040)
val DarkGray500 = Color(0xFF525252)
val DarkGray600 = Color(0xFF656565)
val DarkGray700 = Color(0xFF787878)
val DarkGray800 = Color(0xFF8B8B8B)
val DarkGray900 = Color(0xFF9E9E9E)

// ===== COLORES DE TEXTO ADAPTATIVOS =====
// Modo claro
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)

// Modo oscuro
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFFD1D5DB)
val TextTertiaryDark = Color(0xFF9CA3AF)

// ===== COLORES DE FONDO ADAPTATIVOS =====
// Modo claro
val BackgroundPrimary = Color(0xFFFFFFFF)
val BackgroundSecondary = Color(0xFFF9FAFB)
val BackgroundTertiary = Color(0xFFF3F4F6)

// Modo oscuro
val BackgroundPrimaryDark = Color(0xFF0A0A0A)
val BackgroundSecondaryDark = Color(0xFF141414)
val BackgroundTertiaryDark = Color(0xFF1E1E1E)

// ===== COLORES DE SUPERFICIE ADAPTATIVOS =====
// Modo claro
val SurfacePrimary = Color(0xFFFFFFFF)
val SurfaceSecondary = Color(0xFFF9FAFB)
val SurfaceTertiary = Color(0xFFF3F4F6)

// Modo oscuro
val SurfacePrimaryDark = Color(0xFF141414)
val SurfaceSecondaryDark = Color(0xFF1E1E1E)
val SurfaceTertiaryDark = Color(0xFF2D2D2D)

// ===== COLORES DE BORDE ADAPTATIVOS =====
// Modo claro
val BorderPrimary = Color(0xFFE5E7EB)
val BorderSecondary = Color(0xFFF3F4F6)

// Modo oscuro
val BorderPrimaryDark = Color(0xFF374151)
val BorderSecondaryDark = Color(0xFF4B5563)

// ===== COLORES DE MENSAJE MINIMALISTAS =====
val MessageBubbleOwn = MinimalistBlue
val MessageBubbleOther = Color(0xFFFFFFFF)
val MessageBubbleOtherDark = Color(0xFF1E1E1E)
val MessageBackground = Color(0xFFF9FAFB)
val MessageBackgroundDark = Color(0xFF0A0A0A)

// ===== COLORES INFORMATIVOS =====
val InfoBlue = MinimalistBlue
val InfoBlueLight = MinimalistBlueLight
val InfoBlueDark = MinimalistBlueDark

// ===== COLORES DE ESTADO =====
val Success = MinimalistGreen
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

// ===== COLORES DE ACCENT MINIMALISTAS =====
val AccentOrange = Color(0xFFF97316)
val AccentPink = Color(0xFFEC4899)
val AccentTeal = Color(0xFF14B8A6)
val AccentIndigo = Color(0xFF6366F1)

// ===== COLORES DE SOMBRA Y ELEVACIÓN =====
val ShadowLight = Color(0xFF000000).copy(alpha = 0.05f)
val ShadowMedium = Color(0xFF000000).copy(alpha = 0.1f)
val ShadowDark = Color(0xFF000000).copy(alpha = 0.2f)

// ===== COLORES DE GRADIENTE =====
val GradientStart = MinimalistBlue
val GradientEnd = MinimalistPurple
val GradientMutedStart = MinimalistBlueMuted
val GradientMutedEnd = MinimalistPurpleMuted

// ===== COLORES LEGACY PARA COMPATIBILIDAD =====
// Estos colores se mantienen para compatibilidad con componentes existentes
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Border = BorderPrimary
val PrimaryBlue = MinimalistBlue
val PrimaryBlueDark = MinimalistBlueDark
val SecondaryBlue = MinimalistBlueLight
val BluePrimary = MinimalistBlue
val BluePrimaryDark = MinimalistBlueDark
val LightGray = Gray100
val DarkGray = Gray800
val SecondaryGray = Gray600
val VoiceActivation = MinimalistPurple
val AccentPrimary = MinimalistBlue
val AccentSecondary = MinimalistGreen
val AccentTertiary = MinimalistPurple
val SpyBlue = MinimalistBlueDark
val AlertOrange = AccentOrange
val SafeGreen = MinimalistGreen
val Purple80 = MinimalistPurpleLight
val PurpleGrey80 = MinimalistPurpleMuted
val Pink80 = AccentPink
val Purple40 = MinimalistPurpleDark
val PurpleGrey40 = MinimalistPurpleMuted
val Pink40 = AccentPink

// ===== FUNCIONES DE UTILIDAD PARA COLORES ADAPTATIVOS =====
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



