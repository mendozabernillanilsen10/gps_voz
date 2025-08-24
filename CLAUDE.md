# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android chat application with advanced voice recognition capabilities, built with Kotlin and Jetpack Compose. The app uses Firebase for backend services, Hilt for dependency injection, and incorporates Vosk for offline speech recognition.

## Build and Development Commands

### Build Commands
```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.example.demoappchat.ExampleUnitTest"
```

### Development Commands
```bash
# Install debug APK
./gradlew installDebug

# Run with logging (all components)
./gradlew installDebug && adb logcat -s "DemoAppChat"

# Monitor voice services specifically
adb logcat | grep -E "(VoiceService|BackgroundVoiceService|VoskEngine)"

# Generate icons (Python required)
python generate_icons.py

# Extract Vosk model (PowerShell)
powershell -ExecutionPolicy Bypass -File download_vosk_model.ps1

# Test voice commands (PowerShell)
powershell -ExecutionPolicy Bypass -File test_voice_commands.ps1
```

## Architecture

### Clean Architecture Pattern
- **Presentation Layer**: `presentation/` - Compose UI, ViewModels
- **Domain Layer**: `domain/` - Use cases, repositories interfaces, models
- **Data Layer**: `data/` - Repository implementations, services, data sources

### Key Components

#### Voice Recognition System
- **VoiceRecognitionService**: Foreground service for continuous voice monitoring with automatic chat creation
- **BackgroundVoiceService**: Background service for voice command detection when app is closed
- **VoskEngine**: Offline speech recognition using Vosk models
- **VoiceCommandsRepository**: Manages voice commands and actions
- **Audio Pattern Detection**: Real-time audio analysis
- **Automatic Chat Creation**: Creates group chats based on voice commands (emergency, surveillance, general)

#### Firebase Integration
- **Authentication**: Google Sign-In and Firebase Auth
- **Realtime Database**: Chat messages and user data
- **Cloud Messaging**: Push notifications
- **Storage**: Media files (audio, video, images)

#### Dependency Injection (Hilt)
- **FirebaseModule**: Firebase services configuration
- **RepositoryModule**: Repository implementations binding interfaces to implementations
- **VoiceModule**: Voice-related dependencies and Vosk engine configuration
- **UseCaseModule**: Business logic use cases
- **ServiceModule**: Background services and workers

### File Structure Patterns

#### Presentation Layer
```
presentation/
├── auth/           # Authentication screens
├── chat/           # Chat functionality
├── components/     # Reusable UI components
├── main/           # Main navigation
├── settings/       # App settings
└── splash/         # Splash screen
```

#### Data Layer
```
data/
├── local/          # Local storage (preferences)
├── model/          # Data models
├── repository/     # Repository implementations
├── service/        # Background services
└── webrtc/         # WebRTC client for calls
```

## Important Development Notes

### Voice Model Setup
The Vosk speech recognition model is stored in `app/src/main/assets/vosk-model/`. The model is automatically extracted during app initialization through `MyApplication.extractVoskModel()`.

### Permissions
The app requires multiple sensitive permissions:
- `RECORD_AUDIO` - Voice recognition and audio recording
- `CAMERA` - Video recording and capture
- `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` - Location services for proximity chats
- `POST_NOTIFICATIONS` - Push notifications (Android 13+)
- `WAKE_LOCK` - Keep services active in background
- `FOREGROUND_SERVICE_*` - Multiple foreground service types (microphone, camera, location)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Prevent system from killing services
- `SYSTEM_ALERT_WINDOW` - Display overlay windows

### Build Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 14)
- **Compile SDK**: 35
- **NDK Filters**: arm64-v8a, armeabi-v7a, x86, x86_64 (for Vosk)
- **Java Version**: 1.8
- **Kotlin Compiler Extension**: 1.5.1

### Firebase Setup
Ensure `google-services.json` is present in the `app/` directory. Firebase rules can be applied using:
```bash
powershell -ExecutionPolicy Bypass -File apply_firebase_rules_fixed.ps1
```

### Background Services and Voice Commands
The app uses multiple services for 24/7 voice recognition:
- **Foreground service**: `VoiceRecognitionService` for active voice monitoring
- **Background service**: `BackgroundVoiceService` for voice command detection when app is closed
- **Automatic chat creation**: Voice commands automatically create group chats with configured radius
- **Battery optimization**: Exemption automatically requested through `MainActivity.requestBatteryOptimizationExemption()`

#### Automatic Voice Commands
| Command | Action | Chat Radius | Description |
|---------|--------|-------------|-------------|
| "emergencia", "ayuda", "socorro" | Emergency chat | 5km | Critical situations |
| "alerta" | Alert chat | 3km | Important alerts |
| "vigilancia", "observar", "monitorear" | Surveillance chat | 4km | Monitoring |
| "grabar", "audio", "sonido" | Recording chat | 2km | Audio recording |
| "chat grupal", "grupo", "conversar" | General chat | 3km | General conversations |

## Common Issues and Solutions

### Compilation Issues
If encountering type inference or overload resolution errors:
1. Specify explicit types in variables and function parameters
2. Check for duplicate function definitions
3. Ensure proper import statements
4. Clean and rebuild the project

### Voice Recognition Issues
1. Verify Vosk model is properly extracted in app's internal storage
2. Check microphone permissions are granted
3. Ensure the app has battery optimization exemption
4. Monitor LogCat for VoiceRecognitionService messages

### Firebase Issues
1. Verify `google-services.json` is up to date
2. Check Firebase project configuration
3. Ensure proper Firebase rules are applied
4. Verify internet connectivity for authentication

## Debugging

### Logging Tags
- `VoiceRecognitionService` - Voice service operations and automatic chat creation
- `BackgroundVoiceService` - Background voice command detection
- `VoskEngine` - Speech recognition engine
- `MyApplication` - App initialization and model extraction
- `MainActivity` - Activity lifecycle and permissions
- `FirebaseRepository` - Database operations and notifications

### Common Debug Commands
```bash
# Monitor voice services (foreground and background)
adb logcat -s "VoiceRecognitionService" -s "BackgroundVoiceService"

# Monitor Vosk engine
adb logcat -s "VoskEngine"

# Monitor all app logs
adb logcat -s "DemoAppChat"

# Monitor voice command system specifically
adb logcat | grep -E "(VoiceService|BackgroundVoiceService|FirebaseRepo|automatic|voice command)"

# Monitor chat creation and notifications
adb logcat | grep -E "(chat|notification|FCM)"
```

## Dependencies Management

The project uses Gradle Version Catalogs (`gradle/libs.versions.toml`) for dependency management. Key libraries include:
- **Jetpack Compose** (2024.10.01 BOM) - Modern declarative UI
- **Hilt** (2.48) - Dependency injection
- **Firebase** (32.7.0 BOM) - Backend services suite
- **Vosk** (0.3.47) - Offline speech recognition
- **WebRTC** (libjingle 11139) - Voice/video calls
- **CameraX** (1.3.1) - Media capture
- **Coroutines** (1.7.3) - Asynchronous programming
- **Navigation Compose** (2.7.5) - App navigation
- **WorkManager** (2.9.0) - Background task scheduling

## Automatic Voice Command System

### Overview
The app includes an advanced automatic voice command system that operates 24/7, even when the app is closed. When specific voice commands are detected, the system automatically:

1. Creates a group chat based on command type
2. Registers the user in the chat
3. Starts audio recording automatically
4. Notifies nearby users within configured radius

### System Components

#### Voice Command Flow
```
Voice Input → VoiceRecognitionService/BackgroundVoiceService → 
Command Processing → Chat Creation → User Registration → 
Audio Recording → Nearby User Notifications
```

#### Key Services
- **VoiceRecognitionService**: Primary foreground service for active monitoring
- **BackgroundVoiceService**: Handles commands when app is inactive
- **VoiceCommandReceiver**: Broadcast receiver for voice command events

#### Chat Configuration
Each command type has predefined settings:
```kotlin
data class ChatConfig(
    val title: String,        // Chat title
    val description: String,  // Chat description  
    val radius: Int,         // Notification radius in meters
    val pin: String,         // Access PIN
    val category: String     // Chat category
)
```

### Implementation Details

#### Service Management
Both voice services use:
- **WakeLock**: Keeps services active in background
- **Foreground notification**: Prevents system termination
- **Location tracking**: For proximity-based notifications
- **Automatic restart**: Services restart if killed by system

#### Firebase Integration
The system integrates with Firebase for:
- **Chat creation**: Real-time database storage
- **User registration**: Automatic participant addition
- **FCM notifications**: Push notifications to nearby users
- **Location queries**: Finding users within specified radius

#### Permissions Required
- `RECORD_AUDIO` - Voice recognition
- `ACCESS_FINE_LOCATION` - Precise location for proximity
- `WAKE_LOCK` - Background service operation
- `VIBRATE` - Notification feedback
- `POST_NOTIFICATIONS` - Push notifications (Android 13+)

### Development Notes

#### Testing Voice Commands
Use the included PowerShell scripts:
```bash
# Test voice command detection
powershell -ExecutionPolicy Bypass -File test_voice_commands.ps1

# Test voice system integration
powershell -ExecutionPolicy Bypass -File test_voice_system.ps1
```

#### Customizing Commands
To add new voice commands:
1. Update command mappings in `MainActivity.setupAutomaticVoiceCommands()`
2. Add chat configuration in `BackgroundVoiceService.defaultChatConfig`
3. Configure notification radius in `getNotificationRadiusForChatType()`

#### Troubleshooting Voice Commands
Common issues and solutions:
- **Commands not detected**: Check microphone permissions and service status
- **Chats not created**: Verify Firebase connectivity and user authentication
- **Notifications not sent**: Check location permissions and FCM configuration
- **Service stops**: Ensure battery optimization is disabled for the app

## Additional Scripts and Tools

### PowerShell Scripts (Windows Development)
```bash
# Extract Vosk speech model from assets
powershell -ExecutionPolicy Bypass -File download_vosk_model.ps1

# Test voice command detection system
powershell -ExecutionPolicy Bypass -File test_voice_commands.ps1

# Test complete voice system integration  
powershell -ExecutionPolicy Bypass -File test_voice_system.ps1

# Apply Firebase security rules
powershell -ExecutionPolicy Bypass -File apply_firebase_rules_fixed.ps1

# Get SHA1 fingerprint for Firebase setup
powershell -ExecutionPolicy Bypass -File get_sha1_fingerprint.ps1

# Test on Honor X6B Plus device specifically
powershell -ExecutionPolicy Bypass -File test_honor_device.ps1
```

### Python Utilities
```bash
# Generate app icons in multiple resolutions
python generate_icons.py
```