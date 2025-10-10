# 📹 Cómo Usar las Videollamadas Grupales

## ✅ Respuesta: **SÍ, TODOS SE VEN EN PANTALLA**

Cuando inicias una videollamada, **TODOS los participantes aparecen en un grid** donde pueden verse entre sí.

---

## 🎨 Cómo Se Ve

### Ejemplo con 4 personas en llamada:

```
┌─────────────────────────────────────┐
│    💬 Emergencia Grupo A             │
│    🔵 4 participantes conectados     │
└─────────────────────────────────────┘

    ┌─────────────┐  ┌─────────────┐
    │   👤  J     │  │   👤  M     │
    │             │  │             │  <- Cada cuadro muestra
    │   Juan      │  │   María     │     el video en vivo
    │   🎤        │  │   🔇  📹    │     del participante
    └─────────────┘  └─────────────┘

    ┌─────────────┐  ┌─────────────┐
    │   👤  P     │  │   👤  L     │
    │             │  │             │
    │   Pedro     │  │   Luis      │
    │   🎤        │  │   🎤        │
    └─────────────┘  └─────────────┘


       🔊       📹       🎤          <- Tus controles
     Altavoz  Cámara   Micro

             ⭕ ☎️                   <- Colgar
           COLGAR
```

---

## 🚀 Cómo Agregar el Botón en tu Chat

### Opción 1: Botón en el TopAppBar

Agrega esto en tu archivo `chat.kt`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    recordingViewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleScope = rememberCoroutineScope()
    val chat by viewModel.currentChat.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chat?.title ?: "Chat") },
                actions = {
                    // ⭐ NUEVO: Botón de Videollamada
                    IconButton(
                        onClick = {
                            lifecycleScope.launch {
                                CallHelper.startVideoCall(
                                    context = context,
                                    chatId = chatId,
                                    chatName = chat?.title ?: "Chat Grupal"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videollamada",
                            tint = Color(0xFF007AFF) // Azul iOS
                        )
                    }
                    
                    // ⭐ NUEVO: Botón de Llamada de Audio
                    IconButton(
                        onClick = {
                            lifecycleScope.launch {
                                CallHelper.startAudioCall(
                                    context = context,
                                    chatId = chatId,
                                    chatName = chat?.title ?: "Chat Grupal"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Llamada de audio",
                            tint = Color(0xFF34C759) // Verde iOS
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // ... resto de tu UI
    }
}
```

### Opción 2: FAB Flotante (Más Visible)

```kotlin
Scaffold(
    topBar = { /* Tu TopAppBar */ },
    floatingActionButton = {
        // ⭐ Botón flotante de videollamada
        FloatingActionButton(
            onClick = {
                lifecycleScope.launch {
                    CallHelper.startVideoCall(
                        context = context,
                        chatId = chatId,
                        chatName = chat?.title ?: "Chat"
                    )
                }
            },
            containerColor = Color(0xFF007AFF), // Azul iOS
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Videocam,
                "Videollamada",
                tint = Color.White
            )
        }
    }
) { paddingValues ->
    // ... resto de tu UI
}
```

### Opción 3: Botones en el Input (Más Accesible)

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    // Botón de Videollamada
    IconButton(
        onClick = {
            lifecycleScope.launch {
                CallHelper.startVideoCall(context, chatId, chatName)
            }
        }
    ) {
        Icon(Icons.Default.Videocam, "Video", tint = Color(0xFF007AFF))
    }
    
    // Botón de Llamada de Audio
    IconButton(
        onClick = {
            lifecycleScope.launch {
                CallHelper.startAudioCall(context, chatId, chatName)
            }
        }
    ) {
        Icon(Icons.Default.Call, "Audio", tint = Color(0xFF34C759))
    }
    
    // Tu TextField de mensaje
    OutlinedTextField(
        value = messageText,
        onValueChange = { messageText = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Mensaje...") }
    )
    
    // Botón de enviar
    IconButton(onClick = { /* enviar */ }) {
        Icon(Icons.Default.Send, "Enviar")
    }
}
```

---

## 📱 Flujo Completo de Uso

### 1️⃣ Usuario A Inicia Videollamada

```kotlin
// Usuario A toca el botón 📹
Button(onClick = {
    lifecycleScope.launch {
        val result = CallHelper.startVideoCall(
            context,
            "chat_123",
            "Emergencia Sector Norte"
        )
        
        result.onSuccess {
            Log.d("Call", "✅ Llamada iniciada")
        }
    }
}) {
    Icon(Icons.Default.Videocam, "Video")
    Text("Videollamada")
}
```

### 2️⃣ Notificaciones a Todos

Automáticamente se envía notificación push a:
- ✅ Usuario B
- ✅ Usuario C  
- ✅ Usuario D
- ✅ ... todos los del chat

### 3️⃣ Usuarios Reciben Notificación

```
📱 PANTALLA DEL MÓVIL:

┌─────────────────────────────────┐
│ 📹 Emergencia Sector Norte      │
│                                 │
│ Juan Pérez te está llamando     │
│                                 │
│  [Unirse]          [Ignorar]    │
└─────────────────────────────────┘

🔊 SUENA EL TELÉFONO
📳 VIBRA
```

### 4️⃣ Todos se Unen

Cuando tocan **"Unirse"**:

✅ Se abre `GroupCallActivity`  
✅ Ven el grid con todos los participantes  
✅ Pueden activar/desactivar su cámara  
✅ Pueden silenciar su micrófono  
✅ Escuchan y ven a todos en tiempo real

---

## 🎛️ Controles Disponibles

### Para Cada Usuario:

```
🔊 ALTAVOZ
   └─ ON:  Audio sale por altavoz
   └─ OFF: Audio sale por auricular

📹 CÁMARA (solo videollamada)
   └─ ON:  Los demás te ven
   └─ OFF: Los demás ven tu avatar

🎤 MICRÓFONO
   └─ ON:  Los demás te escuchan
   └─ OFF: Estás silenciado

☎️ COLGAR
   └─ Abandonas la llamada
```

---

## 📊 Indicadores en Pantalla

### En cada tarjeta de participante verás:

```kotlin
┌─────────────┐
│   👤  J     │  <- Avatar del usuario
│             │
│   Juan      │  <- Nombre
│   🎤        │  <- 🎤 = Hablando | 🔇 = Silenciado
│   📹        │  <- 📹 = Video ON | Sin icono = Video OFF
└─────────────┘
```

### Animaciones:

- **Al hablar**: El cuadro pulsa suavemente (escala 1.0 → 1.05)
- **Al unirse**: Aparece con fade in
- **Al salir**: Desaparece con fade out

---

## 🔧 Ejemplo Completo de Implementación

Aquí te doy el código completo para agregar en tu `chat.kt`:

```kotlin
import com.example.demoappchat.utils.CallHelper
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Call

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleScope = rememberCoroutineScope()
    val chat by viewModel.currentChat.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = chat?.title ?: "Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* volver */ }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // 📹 Botón de Videollamada
                    IconButton(
                        onClick = {
                            lifecycleScope.launch {
                                val result = CallHelper.startVideoCall(
                                    context = context,
                                    chatId = chatId,
                                    chatName = chat?.title ?: "Chat"
                                )
                                
                                result.onSuccess { callId ->
                                    Log.d("Chat", "✅ Videollamada iniciada: $callId")
                                }.onFailure { error ->
                                    Log.e("Chat", "❌ Error: ${error.message}")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Iniciar videollamada",
                            tint = Color(0xFF007AFF) // Azul iOS
                        )
                    }
                    
                    // ☎️ Botón de Llamada de Audio
                    IconButton(
                        onClick = {
                            lifecycleScope.launch {
                                CallHelper.startAudioCall(
                                    context = context,
                                    chatId = chatId,
                                    chatName = chat?.title ?: "Chat"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Iniciar llamada de audio",
                            tint = Color(0xFF34C759) // Verde iOS
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ... resto de tu UI (mensajes, input, etc)
        }
    }
}
```

---

## ✅ Características Implementadas

| Característica | Estado | Descripción |
|---------------|--------|-------------|
| **Grid de participantes** | ✅ | Todos se ven en cuadrícula |
| **Adaptable a cantidad** | ✅ | 1x1, 2x2, 3x3 según usuarios |
| **Controles iOS** | ✅ | Micro, cámara, altavoz, colgar |
| **Notificaciones push** | ✅ | Con sonido y vibración |
| **Indicadores de estado** | ✅ | Micrófono, cámara, conexión |
| **Animaciones suaves** | ✅ | Pulso al hablar, fade in/out |
| **Modo audio y video** | ✅ | Ambos tipos de llamada |
| **Pantalla bloqueada** | ✅ | Aparece aunque esté bloqueado |

---

## 🎯 Respuesta Final

**SÍ, TODOS LOS USUARIOS SE VEN EN PANTALLA** 🎉

- ✅ **Grid adaptable**: 1-2-4-6-9 usuarios
- ✅ **Video en tiempo real**: Cada usuario en su cuadro
- ✅ **Indicadores claros**: Quién habla, quién está silenciado
- ✅ **Controles individuales**: Cada uno controla su micro/cámara
- ✅ **Diseño iOS**: Minimalista y elegante
- ✅ **Notificaciones**: Con sonido cuando alguien llama

---

## 🧪 Pruébalo Ahora

1. **Agrega el botón** en tu `chat.kt` (usa el código de arriba)
2. **Compila**: `./gradlew assembleDebug`
3. **Abre un chat** en la app
4. **Toca el botón 📹**
5. **¡Listo!** Se abrirá la pantalla con el grid

---

## 📸 Vista Previa

```
┌────────────────────────────────────┐
│ ◀  Emergencia Grupo A     📹  ☎️  │  <- Botones en TopAppBar
├────────────────────────────────────┤
│                                    │
│  [Mensajes del chat aquí]          │
│                                    │
│                                    │
└────────────────────────────────────┘

Cuando tocas 📹:
↓

┌────────────────────────────────────┐
│    Emergencia Grupo A               │
│    🔵 Conectando...                 │
├────────────────────────────────────┤
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │  J   │  │  M   │  │  P   │     │  <- TODOS SE VEN
│  │ Juan │  │ María│  │ Pedro│     │
│  └──────┘  └──────┘  └──────┘     │
│                                    │
│     🔊      📹      🎤              │
│   Altavoz  Cámara  Micro           │
│                                    │
│           ⭕ ☎️                     │
│         COLGAR                     │
└────────────────────────────────────┘
```

**¡Ahora solo compila y prueba!** 🚀





