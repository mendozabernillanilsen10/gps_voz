package com.example.demoappchat.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demoappchat.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ModernSplashScreen(
    onSplashFinished: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var showContent by remember { mutableStateOf(false) }

    val steps = listOf(
        "Iniciando SafeVoice..." to "🚀",
        "Conectando red segura..." to "🔐",
        "Activando sensores de voz..." to "🎤",
        "¡Todo listo!" to "✨"
    )

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val progressRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "progress_rotation"
    )

    // Control del flujo
    LaunchedEffect(Unit) {
        showContent = true
        delay(500)

        for (i in steps.indices) {
            currentStep = i
            delay(1000)
        }

        delay(500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        White,
                        LightGray,
                        Border.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // Elementos de fondo
        BackgroundShapes()

        if (showContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo principal
                LogoSection(logoScale = logoScale)

                Spacer(modifier = Modifier.height(48.dp))

                // Indicador de progreso circular
                CircularProgressSection(
                    rotation = progressRotation,
                    progress = (currentStep + 1) / steps.size.toFloat()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mensaje actual
                if (currentStep < steps.size) {
                    MessageSection(
                        message = steps[currentStep].first,
                        emoji = steps[currentStep].second
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dots indicadores
                DotsIndicator(currentStep = currentStep, totalSteps = steps.size)
            }
        }
    }
}

@Composable
fun LogoSection(logoScale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo container
        Card(
            modifier = Modifier
                .size(120.dp)
                .scale(logoScale),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryBlue,
                                SpyBlue,
                                VoiceActivation
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = "SafeVoice Logo",
                    modifier = Modifier.size(60.dp),
                    tint = White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Título
        Text(
            text = "SafeVoice",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu asistente de seguridad personal",
            fontSize = 16.sp,
            color = SecondaryGray,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CircularProgressSection(
    rotation: Float,
    progress: Float
) {
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Círculo de fondo
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.width - strokeWidth) / 2

            // Fondo del círculo
            drawCircle(
                color = Border,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Progreso
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PrimaryBlue,
                        SpyBlue,
                        VoiceActivation
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        // Texto del progreso
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}

@Composable
fun MessageSection(
    message: String,
    emoji: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SecondaryGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DotsIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dot_scale_$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(
                        color = if (isActive) PrimaryBlue else Border,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun BackgroundShapes() {
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

    repeat(4) { index ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000 + index * 500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "background_offset_$index"
        )

        Box(
            modifier = Modifier
                .offset(
                    x = (50 + index * 100).dp,
                    y = (150 + index * 200 + offsetY).dp
                )
                .size((30..60).random().dp)
                .background(
                    color = PrimaryBlue.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )
    }
}