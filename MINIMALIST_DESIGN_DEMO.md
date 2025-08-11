# 🚀 App de Chat Minimalista - Guía de Uso

## ✨ Características Principales

### 💬 Chat Básico
- **Envío de mensajes de texto** - Funciona correctamente
- **Mensajes multimedia** - Imágenes, videos y audio
- **Interfaz limpia y moderna** - Diseño Material 3
- **Scroll automático** - Siempre muestra los últimos mensajes

### 📹 Videollamadas Grupales
- **Videollamadas** - Botón en la barra superior
- **Llamadas de audio** - Botón de teléfono
- **Controles de llamada** - Mute, cámara, altavoz
- **Estado de llamada** - Banner que muestra llamada activa

### 🔐 Seguridad
- **Autenticación Firebase** - Login/registro seguro
- **Reglas de base de datos** - Solo usuarios autenticados
- **Validación de datos** - Estructura de mensajes verificada

## 🛠️ Cómo Usar

### 1. Enviar Mensajes
```
1. Abre un chat
2. Escribe tu mensaje en el campo de texto
3. Presiona el botón de enviar (▶️)
4. El mensaje aparecerá inmediatamente
```

### 2. Enviar Media
```
1. Toca el botón de clip (📎)
2. Selecciona el tipo de media:
   - 📷 Imagen (galería)
   - 🎥 Video (grabar)
   - 🎤 Audio (grabar)
3. El archivo se subirá y enviará automáticamente
```

### 3. Iniciar Videollamada Grupal
```
1. En la barra superior del chat, toca:
   - 📹 Para videollamada
   - 📞 Para llamada de audio
2. Se enviará notificación a todos los participantes
3. Aparecerá un banner de llamada activa
4. Usa los controles para gestionar la llamada
```

### 4. Controles de Llamada
```
- 🎤 Mute/Unmute micrófono
- 📹 Encender/Apagar cámara (solo video)
- 🔊 Altavoz on/off
- 🔴 Terminar llamada
- 👁️ Mostrar/ocultar controles
```

## 🔧 Configuración Técnica

### Firebase Rules
Las reglas están configuradas en `firebase_rules.json`:
- ✅ Usuarios autenticados pueden leer/escribir
- ✅ Validación de estructura de mensajes
- ✅ Seguridad por chat y usuario
- ✅ Soporte para llamadas grupales

### Estructura de Datos
```
users/{userId} - Información del usuario
proximity_chats/{chatId} - Chats de proximidad
chat_messages/{chatId}/{messageId} - Mensajes del chat
chat_participants/{chatId}/{userId} - Participantes del chat
group_calls/{callId} - Llamadas grupales activas
```

## 🚨 Solución de Problemas

### Mensajes no se envían
```
✅ Verificar conexión a internet
✅ Verificar autenticación Firebase
✅ Revisar consola para errores
✅ Verificar reglas de Firebase
```

### Videollamadas no funcionan
```
✅ Verificar permisos de cámara/micrófono
✅ Verificar notificaciones FCM
✅ Revisar logs de Firebase
✅ Verificar configuración WebRTC
```

### Media no se sube
```
✅ Verificar permisos de almacenamiento
✅ Verificar conexión a Firebase Storage
✅ Revisar tamaño del archivo
✅ Verificar formato del archivo
```

## 📱 Componentes Principales

### ChatViewModel
- ✅ Gestión de mensajes
- ✅ Envío de media
- ✅ Control de llamadas grupales
- ✅ Manejo de errores

### ChatScreen
- ✅ Interfaz de chat limpia
- ✅ Lista de mensajes optimizada
- ✅ Panel de opciones de media
- ✅ Barra de entrada de mensajes

### VideoCallComposable
- ✅ Interfaz de videollamada
- ✅ Controles de llamada
- ✅ Estados de micrófono/cámara
- ✅ Banner de estado de llamada

## 🎯 Próximas Mejoras

### Funcionalidades Planificadas
- [ ] Notificaciones push mejoradas
- [ ] Grabación de llamadas
- [ ] Compartir pantalla
- [ ] Chat de voz
- [ ] Emojis y stickers

### Optimizaciones Técnicas
- [ ] Paginación de mensajes
- [ ] Cache offline
- [ ] Compresión de media
- [ ] Encriptación end-to-end
- [ ] Sincronización en tiempo real

## 📞 Soporte

Si encuentras problemas:
1. Revisa los logs de Firebase
2. Verifica la consola de Android
3. Comprueba la conectividad
4. Revisa los permisos de la app

---

**Desarrollado con ❤️ usando Kotlin, Jetpack Compose y Firebase**
