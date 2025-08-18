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

# Run with logging
./gradlew installDebug && adb logcat -s "DemoAppChat"

# Generate icons (Python required)
python generate_icons.py

# Extract Vosk model (PowerShell)
powershell -ExecutionPolicy Bypass -File download_vosk_model.ps1
```

## Architecture

### Clean Architecture Pattern
- **Presentation Layer**: `presentation/` - Compose UI, ViewModels
- **Domain Layer**: `domain/` - Use cases, repositories interfaces, models
- **Data Layer**: `data/` - Repository implementations, services, data sources

### Key Components

#### Voice Recognition System
- **VoiceRecognitionService**: Foreground service for continuous voice monitoring
- **VoskEngine**: Offline speech recognition using Vosk models
- **VoiceCommandsRepository**: Manages voice commands and actions
- **Audio Pattern Detection**: Real-time audio analysis

#### Firebase Integration
- **Authentication**: Google Sign-In and Firebase Auth
- **Realtime Database**: Chat messages and user data
- **Cloud Messaging**: Push notifications
- **Storage**: Media files (audio, video, images)

#### Dependency Injection (Hilt)
- **FirebaseModule**: Firebase services configuration
- **RepositoryModule**: Repository implementations
- **VoiceModule**: Voice-related dependencies
- **UseCaseModule**: Business logic use cases

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
- `RECORD_AUDIO` - Voice recognition
- `CAMERA` - Video recording
- `ACCESS_FINE_LOCATION` - Location services
- `POST_NOTIFICATIONS` - Push notifications (Android 13+)

### Build Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 14)
- **Compile SDK**: 35
- **NDK Filters**: arm64-v8a, armeabi-v7a, x86, x86_64 (for Vosk)

### Firebase Setup
Ensure `google-services.json` is present in the `app/` directory. Firebase rules can be applied using:
```bash
powershell -ExecutionPolicy Bypass -File apply_firebase_rules_fixed.ps1
```

### Background Services
The app uses foreground services for voice recognition. Battery optimization exemption is automatically requested through `MainActivity.requestBatteryOptimizationExemption()`.

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
- `VoiceRecognitionService` - Voice service operations
- `VoskEngine` - Speech recognition engine
- `MyApplication` - App initialization and model extraction
- `MainActivity` - Activity lifecycle and permissions

### Common Debug Commands
```bash
# Monitor voice service
adb logcat -s "VoiceRecognitionService"

# Monitor Vosk engine
adb logcat -s "VoskEngine"

# Monitor all app logs
adb logcat -s "DemoAppChat"
```

## Dependencies Management

The project uses Gradle Version Catalogs (`gradle/libs.versions.toml`) for dependency management. Key libraries include:
- Jetpack Compose for UI
- Hilt for dependency injection
- Firebase suite for backend
- Vosk for speech recognition
- WebRTC for voice/video calls
- CameraX for media capture