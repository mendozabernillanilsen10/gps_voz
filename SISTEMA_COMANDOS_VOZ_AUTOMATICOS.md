# Sistema de Comandos de Voz Automáticos

## Descripción General

Este sistema permite que cuando el usuario entre a la aplicación, se inicialicen automáticamente todos los escuchadores y comandos de voz. Cuando el usuario diga un comando específico, la aplicación automáticamente:

1. **Crea un chat grupal** según el tipo de comando
2. **Registra al usuario** en el chat grupal
3. **Comienza a grabar audio** automáticamente
4. **Notifica a usuarios cercanos** configurados por kilómetros

## Comandos de Voz Disponibles

### Comandos de Emergencia
- **"emergencia"** - Crea chat de emergencia (radio: 5km)
- **"ayuda"** - Crea chat de emergencia (radio: 5km)
- **"socorro"** - Crea chat de emergencia (radio: 5km)

### Comandos de Alerta
- **"alerta"** - Crea chat de alerta (radio: 3km)

### Comandos de Vigilancia
- **"vigilancia"** - Crea chat de vigilancia (radio: 4km)
- **"observar"** - Crea chat de vigilancia (radio: 4km)
- **"monitorear"** - Crea chat de vigilancia (radio: 4km)

### Comandos de Grabación
- **"grabar"** - Crea chat de grabación (radio: 2km)
- **"audio"** - Crea chat de grabación (radio: 2km)
- **"sonido"** - Crea chat de grabación (radio: 2km)

### Comandos Generales
- **"chat grupal"** - Crea chat general (radio: 3km)
- **"grupo"** - Crea chat general (radio: 3km)
- **"conversar"** - Crea chat general (radio: 3km)

## Configuración de Radios por Tipo

| Tipo de Chat | Radio | Descripción |
|--------------|-------|-------------|
| Emergencia | 5km | Para situaciones críticas |
| Alerta | 3km | Para alertas importantes |
| Vigilancia | 4km | Para monitoreo y observación |
| Grabación | 2km | Para grabaciones de audio |
| General | 3km | Para conversaciones generales |

## Flujo de Funcionamiento

### 1. Inicialización Automática
Cuando el usuario entra a la aplicación:

```kotlin
// En MainActivity.onCreate()
initializeAllVoiceServices()
```

Esto activa:
- Servicio de reconocimiento de voz principal
- Servicio de voz en segundo plano
- Configuración de comandos automáticos
- Modo 24/7

### 2. Detección de Comando
El sistema escucha continuamente y cuando detecta un comando:

```kotlin
// En VoiceRecognitionService
val automaticCommand = getAutomaticCommandAction(command)
if (automaticCommand != null) {
    handleAutomaticChatCreation(automaticCommand, command)
}
```

### 3. Creación Automática de Chat
El sistema crea automáticamente un chat grupal:

```kotlin
val chatConfig = getChatConfigForAction(action)
val chatId = createGroupChat(chatConfig, originalCommand)
```

### 4. Registro Automático del Usuario
El usuario se registra automáticamente en el chat:

```kotlin
registerUserInChat(chatId)
```

### 5. Grabación Automática de Audio
Comienza la grabación de audio automáticamente:

```kotlin
startAutomaticAudioRecording(chatId)
```

### 6. Notificación a Usuarios Cercanos
Notifica a usuarios dentro del radio configurado:

```kotlin
notifyNearbyUsers(chatId, action, originalCommand)
```

## Estructura de Datos

### ChatConfig
```kotlin
data class ChatConfig(
    val title: String,        // Título del chat
    val description: String,  // Descripción
    val radius: Int,         // Radio en metros
    val pin: String,         // PIN de acceso
    val category: String     // Categoría del chat
)
```

### Datos del Chat en Firebase
```json
{
  "id": "auto_1234567890_123",
  "creatorId": "user_id",
  "creatorName": "Usuario",
  "title": "🚨 Emergencia Automática",
  "description": "Chat de emergencia creado por comando de voz",
  "latitude": -6.758615,
  "longitude": -79.8489161,
  "radius": 5000,
  "pin": "1234",
  "createdAt": 1234567890,
  "isActive": true,
  "participantsCount": 1,
  "lastActivity": 1234567890,
  "category": "emergency",
  "createdByVoice": true,
  "voiceCommand": "emergencia"
}
```

## Servicios Involucrados

### 1. VoiceRecognitionService
- Escucha comandos de voz 24/7
- Procesa comandos automáticos
- Crea chats grupales
- Inicia grabación automática

### 2. BackgroundVoiceService
- Servicio de fondo para detección
- Maneja comandos cuando la app está cerrada
- Notifica a usuarios cercanos

### 3. FirebaseRepository
- Crea chats en Firebase
- Registra usuarios en chats
- Envía notificaciones FCM
- Calcula distancias entre usuarios

## Configuración de Permisos

La aplicación solicita automáticamente:

- `RECORD_AUDIO` - Para grabación de voz
- `ACCESS_FINE_LOCATION` - Para ubicación precisa
- `ACCESS_COARSE_LOCATION` - Para ubicación aproximada
- `WAKE_LOCK` - Para mantener servicios activos
- `POST_NOTIFICATIONS` - Para notificaciones (Android 13+)

## Modo 24/7

El sistema está configurado para funcionar continuamente:

- **WakeLock**: Mantiene el servicio activo
- **Auto-restart**: Se reinicia automáticamente si el sistema lo mata
- **Monitoreo continuo**: Verifica el estado del servicio cada 30 segundos
- **Optimización de batería**: Solicita exención automáticamente

## Notificaciones

### Tipos de Notificación
1. **Notificación de Sistema**: Indica que se está grabando
2. **Notificación de Proximidad**: Para usuarios cercanos
3. **Notificación FCM**: Push notifications

### Contenido de Notificaciones
- Título del chat creado
- Tipo de comando detectado
- Radio de notificación
- Prioridad (alta para emergencias)

## Logs y Debugging

El sistema incluye logs detallados para debugging:

```kotlin
Log.d("VoiceService", "🎯 Comando detectado: $command")
Log.d("VoiceService", "🏗️ Creando chat grupal automático: $action")
Log.d("VoiceService", "✅ Chat grupal creado: $chatId")
Log.d("VoiceService", "🎤 Iniciando grabación automática")
Log.d("VoiceService", "📢 Notificando a usuarios cercanos")
```

## Personalización

### Agregar Nuevos Comandos
Para agregar nuevos comandos, modificar en `MainActivity.setupAutomaticVoiceCommands()`:

```kotlin
val automaticCommands = mapOf(
    "nuevo_comando" to "CREATE_NEW_CHAT_TYPE",
    // ... otros comandos
)
```

### Cambiar Radios de Notificación
Modificar en `BackgroundVoiceService.getNotificationRadiusForChatType()`:

```kotlin
return when (chatType) {
    "nuevo_tipo" -> 6000 // 6km
    // ... otros tipos
}
```

### Configurar Duración de Grabación
Modificar en SharedPreferences:

```kotlin
sharedPreferences.edit().putInt("audio_recording_duration", 10).apply() // 10 segundos
```

## Consideraciones de Seguridad

1. **Autenticación**: Solo usuarios autenticados pueden crear chats
2. **Validación**: Se validan todos los datos antes de crear chats
3. **Permisos**: Se solicitan solo los permisos necesarios
4. **Privacidad**: Las ubicaciones se usan solo para notificaciones de proximidad

## Troubleshooting

### Problemas Comunes

1. **No se detectan comandos**
   - Verificar permisos de micrófono
   - Revisar configuración de sensibilidad
   - Comprobar que el servicio esté activo

2. **No se crean chats**
   - Verificar conexión a Firebase
   - Comprobar autenticación del usuario
   - Revisar logs de error

3. **No se envían notificaciones**
   - Verificar permisos de ubicación
   - Comprobar tokens FCM
   - Revisar configuración de radio

### Logs de Debug
Usar `adb logcat` para ver logs en tiempo real:

```bash
adb logcat | grep -E "(VoiceService|BackgroundVoiceService|FirebaseRepo)"
```
