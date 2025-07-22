package com.example.demoappchat.presentation.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.demoappchat.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()), // Para hacer scroll si el contenido es muy largo
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp)) // Reducido para más espacio

            // Logo simple y clean
            CleanLogo()

            Spacer(modifier = Modifier.height(32.dp))

            // Título más prominente
            Text(
                text = if (isLogin) "Bienvenido de vuelta" else "Crear una cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isLogin) "Ingresa a tu cuenta segura" else "Únete a nuestra red segura",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Formulario
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Campo nombre (solo registro)
                if (!isLogin) {
                    CleanTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Nombre completo",
                        icon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Email
                CleanTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Correo electrónico",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                CleanPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Contraseña",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = !passwordVisible }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón principal más prominente
                CleanButton(
                    text = if (isLogin) "Iniciar Sesión" else "Crear Cuenta",
                    isLoading = uiState.isLoading,
                    enabled = email.isNotBlank() && password.isNotBlank() && (isLogin || name.isNotBlank()),
                    onClick = {
                        if (isLogin) {
                            viewModel.signIn(email, password)
                        } else {
                            viewModel.signUp(email, password, name)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Cambiar modo - MÁS VISIBLE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                isLogin = !isLogin
                                viewModel.clearError()
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLogin) "¿No tienes cuenta? " else "¿Ya tienes cuenta? ",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isLogin) "Crear Cuenta" else "Iniciar Sesión",
                            color = PrimaryBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "O",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones sociales
                SocialLoginButtons()

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                CleanErrorCard(error = error)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CleanLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo circular con gradiente
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(PrimaryBlue, SpyBlue)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = "Logo",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SafeVoice",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Tu asistente de seguridad",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            cursorColor = PrimaryBlue
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityChange) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            cursorColor = PrimaryBlue
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun CleanButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // Aumentado para más prominencia
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 17.sp, // Aumentado
                fontWeight = FontWeight.Bold, // Más bold
                color = Color.White
            )
        }
    }
}

@Composable
fun SocialLoginButtons() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Google
        OutlinedButton(
            onClick = { /* TODO: Google login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continuar con Google",
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Apple
        OutlinedButton(
            onClick = { /* TODO: Apple login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🍎", fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continuar con Apple",
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CleanErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }
    }
}