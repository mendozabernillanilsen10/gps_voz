# 🔥 Configurar Permisos de Firebase para Llamadas

## ❌ Problema Actual

```
Permission denied at /calls/
```

Tu Firebase Database no permite escribir en el nodo `calls`.

---

## ✅ Solución: Actualizar Reglas de Firebase

### 1️⃣ Abre Firebase Console

1. Ve a https://console.firebase.google.com
2. Selecciona tu proyecto **demoappchat-d7407**
3. En el menú lateral, click en **Realtime Database**
4. Click en la pestaña **"Reglas"** (Rules)

---

### 2️⃣ Actualiza las Reglas

Reemplaza las reglas actuales con estas:

```json
{
  "rules": {
    // Usuarios
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "$uid === auth.uid"
      }
    },
    
    // Chats de proximidad
    "chats": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    
    // Participantes de chats
    "chat_participants": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    
    // Mensajes de chats
    "chat_messages": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    
    // ⭐ NUEVO: Llamadas grupales
    "calls": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$callId": {
        ".read": "auth != null",
        ".write": "auth != null",
        "participants": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "status": {
          ".read": "auth != null",
          ".write": "auth != null"
        }
      }
    },
    
    // Tokens FCM
    "fcm_tokens": {
      "$uid": {
        ".read": "auth != null",
        ".write": "$uid === auth.uid"
      }
    },
    
    // Grabaciones
    "recordings": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    
    // Errores de logs
    "errors": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    
    // WebRTC signals (opcional para futuro)
    "webrtc_calls": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    
    "webrtc_signals": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

---

### 3️⃣ Publicar las Reglas

1. Click en el botón **"Publicar"** (Publish) arriba a la derecha
2. Confirma los cambios
3. ¡Listo! ✅

---

## 🧪 Probar de Nuevo

Ahora intenta hacer una llamada desde tu app:

1. Abre un chat
2. Toca el ícono **📹** (videollamada) o **☎️** (audio)
3. Deberías ver en los logs:

```
✅ Llamada registrada en Firebase: call_abc_123
📤 Notificando a usuario: user_2
✅ Total usuarios notificados: 2
```

---

## 📊 Estructura de Datos en Firebase

Después de crear una llamada, verás esto en tu Firebase Database:

```
firebase/
├── calls/
│   └── call_abc_123/
│       ├── callId: "call_abc_123"
│       ├── chatId: "chat_123"
│       ├── initiatorId: "user_1"
│       ├── initiatorName: "Juan Pérez"
│       ├── callType: "VIDEO"
│       ├── status: "RINGING"
│       └── createdAt: 1759800000000
```

---

## 🔒 Reglas de Seguridad Explicadas

### Para Llamadas:

```json
"calls": {
  ".read": "auth != null",      // ✅ Cualquier usuario autenticado puede leer
  ".write": "auth != null",     // ✅ Cualquier usuario autenticado puede escribir
  "$callId": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

**¿Por qué tan permisivo?**
- Es una app policial/emergencias
- Necesitas que cualquier oficial pueda iniciar llamadas
- Los usuarios deben poder unirse rápidamente

---

## 🔐 Reglas Más Restrictivas (Opcional)

Si quieres más seguridad, usa estas reglas:

```json
"calls": {
  ".read": "auth != null",
  "$callId": {
    ".read": "auth != null",
    ".write": "
      auth != null && (
        !data.exists() ||                    // Permitir crear nuevas llamadas
        data.child('initiatorId').val() === auth.uid ||  // El iniciador puede actualizar
        data.child('participants').child(auth.uid).exists()  // Los participantes pueden actualizar
      )
    ",
    "status": {
      ".write": "auth != null"  // Todos pueden cambiar el estado
    },
    "participants": {
      "$userId": {
        ".write": "$userId === auth.uid"  // Solo el usuario puede actualizar su estado
      }
    }
  }
}
```

---

## ❌ Si Aún No Funciona

### 1. Verifica que el usuario esté autenticado

```kotlin
val auth = FirebaseAuth.getInstance()
val currentUser = auth.currentUser

if (currentUser == null) {
    Log.e("Call", "❌ Usuario no autenticado")
    // Redirigir al login
}
```

### 2. Verifica la configuración de Firebase

En `app/google-services.json`, busca:

```json
{
  "project_info": {
    "project_id": "demoappchat-d7407",
    "firebase_url": "https://demoappchat-d7407-default-rtdb.firebaseio.com"
  }
}
```

### 3. Logs Esperados

Cuando funcione correctamente:

```
📞 Iniciando llamada video en chat: Chat Grupal
✅ Llamada registrada en Firebase: call_abc_123
📤 Notificando a usuario: user_2
✅ 1 usuarios notificados
```

---

## 🎉 Resumen

1. **Abrir Firebase Console**
2. **Realtime Database → Reglas**
3. **Copiar las reglas de arriba**
4. **Publicar**
5. **Probar de nuevo en la app**

¡Ahora las llamadas deberían funcionar! 📞✅





