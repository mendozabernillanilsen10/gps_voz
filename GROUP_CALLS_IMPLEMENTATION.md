# Implementación de Llamadas Grupales

## Resumen

Se ha implementado un sistema completo de llamadas grupales para la aplicación de chat de proximidad, incluyendo:

- ✅ **Reglas de Firebase actualizadas** para manejar llamadas grupales
- ✅ **Modelos de datos** para llamadas y participantes
- ✅ **Repositorio de llamadas grupales** con Firebase
- ✅ **UI Components** para mostrar estado de llamadas
- ✅ **Integración con FCM** para notificaciones
- ✅ **Soporte para comandos de voz** para iniciar llamadas

## Estructura de Datos

### Firebase Database

```json
{
  "group_calls": {
    "$chatId": {
      "$callId": {
        "callerId": "string",
        "callerName": "string", 
        "callType": "AUDIO|VIDEO",
        "status": "INITIATING|ACTIVE|ENDED|MISSED",
        "startTime": "timestamp",
        "endTime": "timestamp",
        "maxParticipants": "number",
        "participants": {
          "$userId": {
            "userName": "string",
            "userPhotoUrl": "string",
            "joinTime": "timestamp",
            "leaveTime": "timestamp",
            "isMuted": "boolean",
            "isVideoEnabled": "boolean",
            "connectionStatus": "CONNECTING|CONNECTED|DISCONNECTED|FAILED"
          }
        }
      }
    }
  },
  "call_notifications": {
    "$chatId": {
      "$callId": {
        "recipients": {
          "$userId": {
            "callId": "string",
            "chatId": "string", 
            "timestamp": "timestamp"
          }
        }
      }
    }
  }
}
```

### Modelos de Datos

#### GroupCall
```kotlin
data class GroupCall(
    val callId: String,
    val chatId: String,
    val callerId: String,
    val callerName: String,
    val callType: CallType, // AUDIO, VIDEO
    val status: CallStatus, // INITIATING, ACTIVE, ENDED, MISSED
    val startTime: Long,
    val endTime: Long?,
    val participants: Map<String, CallParticipant>,
    val maxParticipants: Int
)
```

#### CallParticipant
```kotlin
data class CallParticipant(
    val userId: String,
    val userName: String,
    val userPhotoUrl: String,
    val joinTime: Long,
    val leaveTime: Long?,
    val isMuted: Boolean,
    val isVideoEnabled: Boolean,
    val connectionStatus: ConnectionStatus
)
```

## Funcionalidades Implementadas

### 1. Iniciar Llamada Grupal

```kotlin
// En ChatViewModel
fun startGroupVideoCall(chatId: String) {
    viewModelScope.launch {
        repository.startGroupCallWithNotifications(
            chatId = chatId,
            callType = "video_call",
            callerName = user.name
        )
    }
}
```

### 2. Unirse a Llamada

```kotlin
// En GroupCallRepository
suspend fun joinCall(chatId: String, callId: String): Result<CallParticipant>
```

### 3. Salir de Llamada

```kotlin
// En GroupCallRepository  
suspend fun leaveCall(chatId: String, callId: String): Result<Unit>
```

### 4. Terminar Llamada

```kotlin
// En GroupCallRepository
suspend fun endCall(chatId: String, callId: String): Result<Unit>
```

## Componentes UI

### GroupCallStatusBar
Muestra el estado de una llamada grupal activa con opciones para unirse o terminar.

### IncomingCallNotification  
Muestra notificaciones de llamadas entrantes con botones para aceptar/rechazar.

## Integración con Comandos de Voz

Los comandos de voz ya están configurados para iniciar llamadas:

- **"emergencia"** → Inicia llamada automática
- **"socorro"** → Inicia llamada automática  
- **"llamada"** → Inicia llamada automática

## Reglas de Firebase

### Estructura de Reglas

```json
{
  "rules": {
    "group_calls": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null",
        "$callId": {
          ".validate": "newData.hasChildren(['callerId', 'callType', 'status', 'startTime'])",
          "participants": {
            "$uid": {
              ".read": "auth != null",
              ".write": "auth != null"
            }
          },
          "status": {
            ".validate": "newData.isString() && newData.val() in ['initiating', 'active', 'ended', 'missed']"
          },
          "callType": {
            ".validate": "newData.isString() && newData.val() in ['audio', 'video']"
          }
        }
      }
    }
  }
}
```

### Validaciones

- ✅ Solo usuarios autenticados pueden leer/escribir llamadas
- ✅ Validación de campos requeridos para llamadas
- ✅ Validación de estados de llamada permitidos
- ✅ Validación de tipos de llamada permitidos

## Notificaciones FCM

### Estructura de Notificación

```json
{
  "type": "video_call|audio_call",
  "chat_id": "string",
  "caller_name": "string", 
  "call_id": "string",
  "participant_id": "string",
  "timestamp": "string"
}
```

### Manejo de Notificaciones

Las notificaciones se envían automáticamente a todos los participantes del chat cuando se inicia una llamada grupal.

## Próximos Pasos

### 1. Integración con WebRTC
- Implementar conexiones P2P reales
- Manejo de streams de audio/video
- Gestión de calidad de conexión

### 2. Mejoras de UI
- Indicadores de calidad de conexión
- Lista de participantes en tiempo real
- Controles de audio/video individuales

### 3. Funcionalidades Avanzadas
- Grabación de llamadas
- Transmisión de pantalla
- Chat durante llamadas
- Llamadas privadas entre participantes

### 4. Optimizaciones
- Compresión de audio/video
- Adaptación de calidad según conexión
- Manejo de reconexión automática

## Archivos Modificados/Creados

### Nuevos Archivos
- `app/src/main/java/com/example/demoappchat/data/model/GroupCall.kt`
- `app/src/main/java/com/example/demoappchat/data/repository/GroupCallRepository.kt`
- `app/src/main/java/com/example/demoappchat/presentation/components/GroupCallStatusBar.kt`
- `firebase_rules.json`

### Archivos Modificados
- `app/src/main/java/com/example/demoappchat/data/model/ChatMessage.kt` - Agregado tipo CALL
- `app/src/main/java/com/example/demoappchat/domain/model/Chat.kt` - Agregado tipo CALL
- `app/src/main/java/com/example/demoappchat/data/repository/FirebaseRepository.kt` - Métodos de llamadas
- `app/src/main/java/com/example/demoappchat/presentation/chat/ChatViewModel.kt` - Estado de llamadas
- `app/src/main/java/com/example/demoappchat/presentation/chat/chat.kt` - UI de llamadas

## Testing

### Casos de Prueba Recomendados

1. **Iniciar llamada grupal**
   - Verificar que se crea en Firebase
   - Verificar que se envían notificaciones
   - Verificar que se actualiza la UI

2. **Unirse a llamada**
   - Verificar que se agrega como participante
   - Verificar que se actualiza el estado

3. **Salir de llamada**
   - Verificar que se marca como desconectado
   - Verificar que se termina si no quedan participantes

4. **Comandos de voz**
   - Verificar que "emergencia" inicia llamada
   - Verificar que se envía mensaje de tipo CALL

## Configuración de Firebase

### 1. Actualizar Reglas de Database
Copiar el contenido de `firebase_rules.json` a las reglas de Firebase Database.

### 2. Verificar Permisos
Asegurarse de que los usuarios tengan permisos para:
- Leer/escribir en `group_calls`
- Leer/escribir en `call_notifications`
- Enviar notificaciones FCM

### 3. Configurar FCM
Verificar que las notificaciones FCM estén configuradas correctamente para el envío de notificaciones de llamadas. 