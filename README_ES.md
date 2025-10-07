# SafeVoice Chat - Sistema de Comunicación por Proximidad con Control por Voz

Una sofisticada aplicación Android con capacidades avanzadas de reconocimiento de voz, chats basados en proximidad geográfica y comunicación en tiempo real potenciada por Firebase.

---

## 📱 Descripción General

**SafeVoice Chat** (DemoAppChat) es una aplicación Android de nivel profesional diseñada para comunicación grupal segura y consciente de la ubicación. La aplicación incluye:

- 🎤 **Reconocimiento de Voz Avanzado** usando Vosk (reconocimiento offline)
- 📍 **Chats por Proximidad** creación y descubrimiento basado en ubicación
- 🔐 **Autenticación Segura** con Firebase Auth
- 💬 **Mensajería en Tiempo Real** con Firebase Realtime Database
- 🎥 **Llamadas de Video y Audio** con integración WebRTC
- 📱 **Compatibilidad Multi-Dispositivo** con optimizaciones específicas por fabricante
- 🌐 **Capacidad Offline** con reconocimiento de voz offline
- ⚡ **Servicios en Segundo Plano** para monitoreo de comandos de voz 24/7

---

## 🎯 Características Principales

### Sistema de Reconocimiento de Voz
- **Reconocimiento de Voz Offline** usando modelos Vosk
- **Comandos de Voz Automáticos** para operación manos libres
- **Configuración Personalizada de Comandos** con sensibilidad configurable
- **Sistema de Respaldo Multi-Motor** para mayor precisión
- **Detección Basada en Patrones** para escenarios de baja confianza

### Sistema de Chats por Proximidad
- **Creación de Chats Basada en Ubicación** con radio personalizable
- **Descubrimiento Automático de Usuarios** dentro de la proximidad
- **Grupos Protegidos por PIN** para seguridad
- **Seguimiento de Participantes en Tiempo Real**
- **Gestión de Expiración Automática** de chats

### Medios y Comunicación
- **Mensajería de Texto** con soporte multimedia rico
- **Mensajes de Voz** con grabación automática
- **Mensajes de Video** y grabación
- **Llamadas de Audio/Video** usando WebRTC
- **Videollamadas Grupales** soporte incluido
- **Compartir Archivos** mediante Firebase Storage

### Compatibilidad de Dispositivos
- **Compatibilidad Universal** con todos los dispositivos Android (API 24+)
- **Optimizaciones Específicas por Fabricante** para:
  - Dispositivos Honor/Huawei
  - Dispositivos Xiaomi (MIUI)
  - Dispositivos Oppo (ColorOS)
  - Dispositivos Vivo (FuntouchOS)
  - Dispositivos Samsung
  - Dispositivos OnePlus
  - Dispositivos Android genéricos

---

## 🛠️ Stack Tecnológico

### Tecnologías Principales
- **Lenguaje**: Kotlin
- **Framework UI**: Jetpack Compose
- **Arquitectura**: MVVM con principios de Arquitectura Limpia
- **Inyección de Dependencias**: Hilt (Dagger 2)
- **Procesamiento Asíncrono**: Kotlin Coroutines & Flow

### Backend y Servicios
- **Autenticación**: Firebase Authentication
- **Base de Datos**: Firebase Realtime Database
- **Almacenamiento**: Firebase Cloud Storage
- **Notificaciones**: Firebase Cloud Messaging (FCM)
- **Analíticas**: Firebase Analytics

### Medios y Comunicación
- **Reconocimiento de Voz**: Vosk (reconocimiento de voz offline)
- **Grabación de Audio**: MediaRecorder API
- **Video**: CameraX + ExoPlayer
- **WebRTC**: WebRTC para llamadas peer-to-peer
- **Ubicación**: Google Play Services Location

---

## 🚀 Comenzando

### Requisitos Previos
- Android Studio Hedgehog (2023.1.1) o superior
- Android SDK API 24 (Android 7.0) o superior
- JDK 8 o superior
- Proyecto Firebase con:
  - Authentication habilitado
  - Realtime Database configurado
  - Cloud Storage habilitado
  - FCM habilitado

### Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tuusuario/demoappchat.git
   cd demoappchat
   ```

2. **Configurar Firebase**
   - Crear un proyecto Firebase en [Firebase Console](https://console.firebase.google.com)
   - Descargar `google-services.json`
   - Colocarlo en el directorio `app/`
   - Actualizar reglas de Firebase usando `firebase_rules.json`

3. **Configurar Modelo Vosk**
   - La app descarga automáticamente el modelo Vosk en español al iniciar por primera vez
   - O descarga manualmente desde [Modelos Vosk](https://alphacephei.com/vosk/models)
   - Colocar en `app/src/main/assets/vosk-model/`

4. **Compilar y Ejecutar**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

---

## 🎮 Uso

### Configuración Inicial

1. **Primer Inicio**
   - Otorgar permisos requeridos (Micrófono, Ubicación, Cámara)
   - Permitir exención de optimización de batería
   - Completar configuración específica del dispositivo

2. **Autenticación**
   - Iniciar sesión con email/contraseña o Google
   - El perfil se crea automáticamente

3. **Crear Ubicación**
   - Otorgar permisos de ubicación
   - Tu ubicación se usa para descubrir chats por proximidad

### Comandos de Voz

La aplicación soporta comandos de voz automáticos para operación manos libres:

#### Comandos de Emergencia (radio 5km)
- **"emergencia"** - Crea chat de emergencia
- **"ayuda"** - Crea chat de ayuda
- **"socorro"** - Crea chat de SOS

#### Comandos de Alerta (radio 3km)
- **"alerta"** - Crea chat de alerta

#### Comandos de Vigilancia (radio 4km)
- **"vigilancia"** - Crea chat de vigilancia
- **"observar"** - Crea chat de observación
- **"monitorear"** - Crea chat de monitoreo

#### Comandos de Grabación (radio 2km)
- **"grabar"** - Crea chat de grabación
- **"audio"** - Crea chat de audio
- **"sonido"** - Crea chat de sonido

#### Comandos Generales (radio 3km)
- **"chat grupal"** - Crea chat grupal general
- **"grupo"** - Crea chat de grupo
- **"conversar"** - Crea chat de conversación

### Crear Chats por Proximidad

1. **Creación Manual**
   - Tocar botón "Crear Chat"
   - Ingresar nombre y descripción del chat
   - Establecer radio (500m - 10km)
   - Establecer PIN (4 dígitos)
   - Seleccionar categoría

2. **Creación por Voz**
   - Decir cualquier comando de voz
   - El chat se crea automáticamente
   - Te registras automáticamente
   - La grabación de audio inicia automáticamente
   - Los usuarios cercanos son notificados

### Unirse a Chats

1. **Descubrimiento Automático**
   - Los chats cercanos aparecen automáticamente
   - Basado en tu ubicación GPS

2. **Unirse Manualmente**
   - Seleccionar chat de la lista
   - Ingresar PIN
   - Unirse y comenzar a comunicarse

---

## 📱 Arquitectura de la Aplicación

### Estructura del Proyecto
```
app/
├── src/main/
│   ├── java/com/example/demoappchat/
│   │   ├── data/                    # Capa de datos
│   │   │   ├── model/              # Modelos de datos
│   │   │   ├── repository/         # Repositorios
│   │   │   ├── service/            # Servicios
│   │   │   │   ├── voice/          # Motores de reconocimiento de voz
│   │   │   │   ├── VoiceRecognitionService.kt
│   │   │   │   ├── BackgroundVoiceService.kt
│   │   │   │   └── SimpleMediaRecordingService.kt
│   │   │   ├── receiver/           # Receptores de broadcast
│   │   │   └── webrtc/             # Implementación WebRTC
│   │   ├── domain/                 # Capa de dominio
│   │   │   ├── model/              # Modelos de dominio
│   │   │   └── usecase/            # Casos de uso
│   │   ├── presentation/           # Capa de presentación
│   │   │   ├── auth/               # Pantallas de autenticación
│   │   │   ├── main/               # Pantalla principal
│   │   │   ├── chat/               # Pantallas de chat
│   │   │   ├── settings/           # Pantallas de configuración
│   │   │   └── components/         # Componentes reutilizables
│   │   ├── di/                     # Inyección de dependencias
│   │   ├── ui/theme/               # Tema y estilos
│   │   ├── utils/                  # Utilidades
│   │   │   ├── DeviceCompatibilityManager.kt
│   │   │   └── NavigationHelper.kt
│   │   ├── MainActivity.kt
│   │   └── MyApplication.kt
│   ├── res/                        # Recursos
│   └── AndroidManifest.xml
└── build.gradle.kts
```

---

## 🔧 Configuración Avanzada

### Ajustes de Reconocimiento de Voz

Ajustar reconocimiento de voz en Configuración:
- **Sensibilidad**: 0-100% (por defecto: 70%)
- **Duración de Grabación**: 1-60 segundos
- **Auto-grabación**: Habilitar/deshabilitar
- **Modo Sigiloso**: Grabación oculta
- **Modo 24/7**: Escucha siempre activa

### Canales de Notificación

La app usa múltiples canales de notificación:
- **Reconocimiento de Voz** (Prioridad alta)
- **Alertas de Emergencia** (Urgente)
- **Mensajes de Chat** (Por defecto)
- **Servicio en Segundo Plano** (Prioridad baja)

---

## 🐛 Solución de Problemas

### Los Comandos de Voz No Funcionan

**Problema**: Los comandos de voz no se detectan

**Soluciones**:
1. Verificar permisos de micrófono
2. Verificar que el modelo Vosk esté instalado
3. Ajustar sensibilidad en Configuración
4. Verificar optimizaciones específicas del dispositivo
5. Reiniciar servicio de voz

**Mejoras Recientes**:
- ✅ Normalización de texto en español (elimina acentos)
- ✅ Múltiples estrategias de coincidencia
- ✅ Detección mejorada de palabras clave en español
- ✅ Umbral de confianza reducido (70% del original)
- ✅ Mejor extracción de JSON de Vosk

### Los Chats No Aparecen

**Problema**: Los chats cercanos no se muestran

**Soluciones**:
1. Habilitar servicios de ubicación
2. Otorgar permisos de ubicación
3. Verificar señal GPS
4. Verificar conexión Firebase
5. Verificar configuración de radio del chat

### La App se Detiene en Segundo Plano (Honor/Xiaomi/Oppo)

**Problema**: La app deja de funcionar cuando la pantalla está apagada

**Soluciones**:
1. Deshabilitar optimización de batería
2. Habilitar permiso de inicio automático
3. Agregar app a apps protegidas
4. Configurar gestión de energía
5. Habilitar exención de restricciones en segundo plano

---

## 🔐 Seguridad y Privacidad

### Protección de Datos
- Todas las comunicaciones cifradas en tránsito (TLS)
- Las reglas de seguridad de Firebase hacen cumplir el control de acceso
- Chats grupales protegidos por PIN
- Los datos de ubicación solo se comparten dentro del radio del chat

### Permisos
- **Micrófono**: Comandos de voz y mensajes de audio
- **Cámara**: Mensajes de video y llamadas
- **Ubicación**: Descubrimiento de chats por proximidad
- **Almacenamiento**: Caché de archivos multimedia
- **Notificaciones**: Notificaciones de chat y alertas

---

## 📊 Rendimiento

### Estrategias de Optimización
- Inicialización perezosa de servicios
- Listeners eficientes de Firebase
- Compresión de imágenes para medios
- Gestión de tareas en segundo plano
- Prevención de fugas de memoria

### Optimización de Batería
- Compatibilidad con modo Doze
- JobScheduler para tareas diferidas
- Gestión de wake locks
- Actualizaciones eficientes de ubicación

---

## 📝 Mejoras Recientes en Reconocimiento de Voz

### Sistema de Detección Mejorado

#### 1. Normalización de Texto en Español
```kotlin
// Elimina acentos y normaliza texto
"emergencia" == "emergéncia" == "EMERGENCIA"
```

#### 2. Múltiples Estrategias de Coincidencia
- ✅ **Coincidencia Exacta**: Texto idéntico al comando
- ✅ **Contenido**: El texto contiene el comando
- ✅ **Parcial**: El comando contiene el texto
- ✅ **Similar**: Búsqueda por palabras clave en español

#### 3. Palabras Clave en Español
```kotlin
"ayuda", "socorro", "auxilio" → detecta como "emergencia"
"vigilar", "observar", "controlar" → detecta como "vigilancia"
"grabación", "registrar" → detecta como "grabar"
```

#### 4. Umbral de Confianza Adaptativo
- Umbral original: 70%
- Umbral ajustado: 49% (70% de 70%)
- Mejor detección sin perder precisión

#### 5. Extracción Mejorada de JSON
- Soporta múltiples formatos de respuesta de Vosk
- Maneja "text", "partial" y otros campos
- Registro detallado para debugging

### Cómo Probar las Mejoras

1. **Probar con Acentos**:
   - Di "emergéncia" (con acento)
   - Debería detectar como "emergencia"

2. **Probar Sinónimos**:
   - Di "ayuda" o "socorro"
   - Debería detectar como "emergencia"

3. **Probar Frases Parciales**:
   - Di "quiero grabar audio"
   - Debería detectar "grabar"

4. **Monitorear Logs**:
   ```bash
   adb logcat | grep "SimpleVoskEngine"
   ```

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor sigue estas pautas:

1. Fork el repositorio
2. Crear una rama de característica
3. Hacer tus cambios
4. Escribir/actualizar pruebas
5. Enviar un pull request

---

## 📞 Soporte

Para problemas, preguntas o solicitudes de características:
- Abrir un issue en GitHub
- Revisar documentación
- Verificar la consola de Firebase para problemas de backend

---

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver archivo LICENSE para detalles.

---

## 🙏 Agradecimientos

- **Vosk** - Reconocimiento de voz offline
- **Firebase** - Infraestructura backend
- **WebRTC** - Comunicación en tiempo real
- **Jetpack Compose** - Framework UI moderno
- **Comunidad Android** - Soporte y recursos

---

**Construido con ❤️ para comunicación segura y manos libres**

