# 🕵️ SafeVoice - Demo de App Espía

## 📋 Descripción
SafeVoice es una aplicación de seguridad que funciona como un sistema de vigilancia discreto. Cuando el usuario se encuentra en una reunión o situación de emergencia, la app puede activarse mediante comandos de voz para grabar audio/video y transmitir la información automáticamente.

## 🚀 Funcionalidades Implementadas

### ✅ **Servicio en Segundo Plano**
- El servicio continúa funcionando aunque la app esté en pausa o el teléfono bloqueado
- Usa WakeLock para mantener el dispositivo activo
- Notificación persistente que indica el estado del servicio

### ✅ **Reconocimiento de Voz**
- Integración con Vosk para reconocimiento de voz local
- Comandos personalizables de activación
- Funciona sin conexión a internet

### ✅ **Grabación de Audio/Video**
- Grabación automática según el comando detectado
- Soporte para audio (MP3) y video (MP4)
- Almacenamiento local temporal

### ✅ **Transmisión Automática**
- Subida automática a Firebase Storage
- Envío de mensajes multimedia al chat de emergencia
- Integración con tu estructura de Firebase existente

### ✅ **Modo Discreto**
- Opción para funcionar sin notificaciones visibles
- Grabación silenciosa en segundo plano

## 🎯 Cómo Usar el Demo

### 1. **Configuración Inicial**
1. Abre la app y regístrate/inicia sesión
2. Concede todos los permisos solicitados:
   - Micrófono
   - Cámara
   - Ubicación
   - Almacenamiento
   - Servicio en primer plano

### 2. **Configurar Comandos de Voz**
1. En la pantalla principal, verás el panel "Activación por Voz"
2. Activa el servicio con el switch
3. Configura los comandos de activación:
   - **"óyeme"** o **"alerta"** → Graba audio
   - **"grabar video"** o **"cámara"** → Graba video
   - Puedes agregar comandos personalizados

### 3. **Probar la Funcionalidad**
1. **Simula una situación de emergencia:**
   - Di en voz alta uno de los comandos configurados
   - La app detectará el comando y comenzará a grabar
   - Verás una notificación indicando que está grabando

2. **Verificar la transmisión:**
   - La grabación se sube automáticamente a Firebase Storage
   - Se envía un mensaje al chat de emergencia con el archivo
   - Puedes ver el archivo en la sección de mensajes del chat

### 4. **Modo Discreto**
- Activa "Modo Discreto" para funcionar sin notificaciones visibles
- La app seguirá grabando pero de forma más discreta

## 🔧 Comandos de Prueba

### **Para Audio:**
- "óyeme"
- "alerta"
- "grabar audio"
- "ayuda"

### **Para Video:**
- "grabar video"
- "cámara"
- "video"

## 📱 Estados de la App

### **🟢 Servicio Activo**
- El servicio está funcionando y escuchando comandos
- Aparece en la notificación persistente

### **🔴 Grabando**
- Se está grabando audio o video
- La notificación muestra el tipo de grabación

### **⚪ Servicio Inactivo**
- El servicio está desactivado
- No escucha comandos

## 🗂️ Estructura de Archivos

### **Grabaciones Guardadas:**
- **Audio:** `audio_YYYYMMDD_HHMMSS.mp3`
- **Video:** `video_YYYYMMDD_HHMMSS.mp4`

### **Ubicación:**
- Los archivos se guardan temporalmente en el almacenamiento interno
- Se suben automáticamente a Firebase Storage
- Se envían como mensajes multimedia al chat

## 🔒 Consideraciones de Seguridad

### **⚠️ Uso Legal:**
- Este demo es solo para fines educativos y de prueba
- Asegúrate de cumplir con las leyes de privacidad locales
- Solo usa en situaciones donde tengas consentimiento explícito

### **🔐 Permisos Requeridos:**
- `RECORD_AUDIO` - Para grabación de audio
- `CAMERA` - Para grabación de video
- `FOREGROUND_SERVICE` - Para servicio en segundo plano
- `WAKE_LOCK` - Para mantener el dispositivo activo
- `ACCESS_FINE_LOCATION` - Para ubicación de emergencia

## 🛠️ Personalización

### **Comandos Personalizados:**
- Agrega tus propios comandos de activación
- Los comandos son sensibles a mayúsculas/minúsculas
- Funciona con reconocimiento de voz en español

### **Radio de Emergencia:**
- Configura el radio de búsqueda de chats de emergencia
- Rango: 100m - 1000m

### **Modo Discreto:**
- Activa/desactiva notificaciones visibles
- Útil para situaciones donde necesitas discreción

## 📊 Monitoreo

### **Logs de Debug:**
- Revisa los logs de Android Studio para ver la actividad del servicio
- Busca tags: "VoiceService", "FirebaseRepo"

### **Notificaciones:**
- Notificación persistente del servicio
- Notificación de emergencia cuando se activa
- Notificación de grabación en progreso

## 🚨 Solución de Problemas

### **El servicio no inicia:**
1. Verifica que todos los permisos estén concedidos
2. Reinicia la app
3. Revisa los logs de error

### **No detecta comandos:**
1. Habla más cerca del micrófono
2. Verifica que el comando esté en la lista configurada
3. Asegúrate de que el modelo de Vosk esté descargado

### **No sube archivos:**
1. Verifica la conexión a internet
2. Revisa la configuración de Firebase
3. Verifica los permisos de almacenamiento

## 🎉 ¡Demo Listo!

Tu app espía está completamente funcional. Puedes:
- ✅ Activar grabación por comandos de voz
- ✅ Grabar audio y video en segundo plano
- ✅ Transmitir automáticamente a Firebase
- ✅ Funcionar de forma discreta
- ✅ Integrarse con tu sistema de chats existente

**¡Disfruta probando tu demo de vigilancia!** 🕵️‍♂️ 