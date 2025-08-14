# 🔥 Solución para Error de Índice de Firebase

## ❌ Error Encontrado
```
java.lang.Exception: Index not defined, add ".indexOn": "isActive", for path "/users", to the rules
```

## 🎯 Problema
Firebase Database requiere índices definidos para consultas que usan `orderByChild()`. El error ocurre porque la aplicación está consultando usuarios activos con `orderByChild("isActive").equalTo(true)` pero no hay un índice configurado.

## ✅ Solución Implementada

### 1. **Reglas de Firebase Actualizadas**
He actualizado `firebase_rules.json` con todos los índices necesarios:

```json
{
  "rules": {
    "users": {
      ".indexOn": ["isActive", "lastSeen", "status"],
      // ... resto de reglas
    },
    "proximity_chats": {
      ".indexOn": ["latitude", "longitude", "createdAt", "isActive"],
      // ... resto de reglas
    }
    // ... más índices
  }
}
```

### 2. **Índices Agregados**
- ✅ `users`: `isActive`, `lastSeen`, `status`
- ✅ `proximity_chats`: `latitude`, `longitude`, `createdAt`, `isActive`
- ✅ `chat_messages`: `timestamp`, `userId`, `messageType`
- ✅ `chat_participants`: `joinedAt`, `isActive`
- ✅ `user_locations`: `latitude`, `longitude`, `lastUpdated`
- ✅ `webrtc_calls`: `chatId`, `callType`, `timestamp`, `status`
- ✅ `webrtc_signals`: `callId`, `timestamp`
- ✅ `chat_notifications`: `chatId`, `timestamp`, `type`
- ✅ `fcm_tokens`: `userId`, `deviceId`
- ✅ `notification_logs`: `timestamp`, `userId`, `type`
- ✅ `error_logs`: `timestamp`, `severity`, `userId`
- ✅ `group_calls`: `chatId`, `callType`, `timestamp`, `status`
- ✅ `voice_commands`: `userId`, `command`, `isActive`
- ✅ `voice_settings`: `userId`, `settingType`
- ✅ `media_recordings`: `chatId`, `userId`, `timestamp`, `mediaType`
- ✅ `emergency_alerts`: `userId`, `timestamp`, `status`, `priority`
- ✅ `surveillance_logs`: `userId`, `timestamp`, `actionType`

## 🚀 Cómo Aplicar la Solución

### Opción 1: Script Automático (Recomendado)
```powershell
# Ejecutar el script de PowerShell
.\apply_firebase_rules.ps1
```

### Opción 2: Manual con Firebase CLI
```bash
# 1. Instalar Firebase CLI (si no está instalado)
npm install -g firebase-tools

# 2. Autenticarse en Firebase
firebase login

# 3. Aplicar las reglas
firebase deploy --only database
```

### Opción 3: Consola Web de Firebase
1. Ir a [Firebase Console](https://console.firebase.google.com)
2. Seleccionar tu proyecto
3. Ir a **Realtime Database**
4. Ir a la pestaña **Rules**
5. Copiar y pegar el contenido de `firebase_rules.json`
6. Hacer clic en **Publish**

## 🔍 Consultas Afectadas

### En `FirebaseRepository.kt`:
```kotlin
// ✅ Ahora funciona correctamente con el índice
val usersSnapshot = usersRef.orderByChild("isActive").equalTo(true).get().await()
```

### En `GroupCallRepository.kt`:
```kotlin
// ✅ Consultas de llamadas grupales
callsRef.orderByChild("chatId").equalTo(chatId)
```

### En `WebRTCSignalingService.kt`:
```kotlin
// ✅ Consultas de llamadas WebRTC
callsRef.orderByChild("chatId").equalTo(chatId)
```

## 📊 Beneficios de la Solución

1. **Elimina errores de índice**: Las consultas ahora funcionan correctamente
2. **Mejora el rendimiento**: Los índices optimizan las consultas
3. **Escalabilidad**: La aplicación puede manejar más datos eficientemente
4. **Funcionalidad completa**: Todas las características funcionan sin errores

## 🧪 Verificación

Después de aplicar las reglas, verifica que:

1. ✅ La aplicación compila sin errores
2. ✅ Las consultas de usuarios activos funcionan
3. ✅ Los chats de proximidad se cargan correctamente
4. ✅ Las llamadas grupales funcionan
5. ✅ Las notificaciones se envían sin errores

## 🚨 Notas Importantes

- **Tiempo de propagación**: Los índices pueden tardar unos minutos en propagarse
- **Costo**: Los índices pueden aumentar ligeramente el costo de Firebase
- **Compatibilidad**: Las reglas son compatibles con versiones anteriores de datos

## 📞 Soporte

Si persisten los errores después de aplicar esta solución:

1. Verifica que las reglas se aplicaron correctamente
2. Revisa los logs de Firebase Console
3. Espera 5-10 minutos para la propagación de índices
4. Reinicia la aplicación

---

**¡Con esta solución, el error de índice debería estar completamente resuelto! 🎉**
