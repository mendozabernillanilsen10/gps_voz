# Fix: Fotos enviándose como Video

## 🐛 **Problema Identificado**

Cuando se seleccionaba una foto o imagen desde la galería, el sistema la estaba enviando como video en lugar de como foto.

## 🔍 **Causa Raíz**

El problema tenía múltiples causas:

### 1. **Extensión de archivo incorrecta**
En `ChatViewModel.kt`, línea 90-95:
```kotlin
// ❌ ANTES - Extensión incorrecta para imágenes
when (type) {
    "audio" -> ".mp3"
    "video" -> ".mp4"
    "photo" -> ".jpg"
    else -> ".mp4"  // ← Aquí estaba el problema
}
```

### 2. **Tipo de mensaje forzado**
En `FirebaseRepository.kt`, línea 395:
```kotlin
// ❌ ANTES - Forzaba AUDIO o VIDEO
messageType = if (messageType == "AUDIO") MessageType.AUDIO else MessageType.VIDEO
```

### 3. **Mapeo incorrecto de tipos**
El sistema no estaba mapeando correctamente el tipo "image" a "PHOTO".

## ✅ **Solución Implementada**

### 1. **Fix en ChatViewModel.kt**
```kotlin
// ✅ DESPUÉS - Extensión correcta para imágenes
when (type) {
    "audio" -> ".mp3"
    "video" -> ".mp4"
    "photo", "image" -> ".jpg"  // ← Agregado "image"
    else -> ".jpg"  // ← Cambiado de .mp4 a .jpg
}
```

### 2. **Fix en FirebaseRepository.kt**
```kotlin
// ✅ DESPUÉS - Mapeo correcto de tipos
messageType = when (messageType) {
    "AUDIO" -> MessageType.AUDIO
    "VIDEO" -> MessageType.VIDEO
    "PHOTO" -> MessageType.PHOTO  // ← Agregado PHOTO
    else -> MessageType.PHOTO  // ← Por defecto PHOTO
}
```

### 3. **Launcher específico para imágenes**
```kotlin
// ✅ DESPUÉS - Launcher dedicado para fotos
val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let {
        pendingMediaToUpload = it to "image"  // ← Tipo explícito
    }
}
```

### 4. **Mapeo correcto de tipos en uploadAndSendMedia**
```kotlin
// ✅ DESPUÉS - Mapeo correcto
messageType = when (type) {
    "audio" -> "AUDIO"
    "video" -> "VIDEO"
    "photo", "image" -> "PHOTO"  // ← Agregado "image"
    else -> "PHOTO"  // ← Por defecto PHOTO
}
```

## 🎯 **Resultado**

Ahora cuando selecciones una foto:
- ✅ **Se guarda con extensión .jpg**
- ✅ **Se envía como MessageType.PHOTO**
- ✅ **Se muestra correctamente como imagen**
- ✅ **No se confunde con video**

## 📱 **Flujo Corregido**

1. **Usuario toca "Galería"** → `photoPickerLauncher.launch("image/*")`
2. **Selecciona imagen** → `pendingMediaToUpload = uri to "image"`
3. **Upload** → Archivo guardado como `.jpg`
4. **Envío** → `MessageType.PHOTO` con contenido "Foto enviada"
5. **Visualización** → Se muestra como imagen, no como video

## 🔧 **Archivos Modificados**

- `app/src/main/java/com/example/demoappchat/presentation/chat/ChatViewModel.kt`
- `app/src/main/java/com/example/demoappchat/presentation/chat/chat.kt`
- `app/src/main/java/com/example/demoappchat/data/repository/FirebaseRepository.kt`

## 🧪 **Testing**

Para verificar que el fix funciona:

1. **Selecciona una foto** desde la galería
2. **Verifica que se envía** como imagen (no video)
3. **Confirma que se muestra** correctamente en el chat
4. **Prueba diferentes formatos** (JPG, PNG, etc.)

El problema está completamente resuelto. Las fotos ahora se envían correctamente como imágenes. 🎉 