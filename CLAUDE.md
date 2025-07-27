# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android chat application built with Kotlin and Jetpack Compose that includes proximity-based chat functionality, Firebase integration for real-time messaging, and voice recognition capabilities using Vosk library. The app features background services for voice command detection and media recording.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run connected (instrumented) tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Install debug APK to connected device
./gradlew installDebug
```

## Architecture

### Core Structure
- **MVVM Architecture**: ViewModels handle UI state, Repositories manage data operations
- **Hilt Dependency Injection**: All major components use `@Inject` and `@AndroidEntryPoint`
- **Jetpack Compose**: Modern UI toolkit with navigation compose
- **Firebase Integration**: Realtime Database, Auth, Storage, and Analytics
- **Background Services**: Foreground service for voice recognition using Vosk

### Key Components

**Application Layer** (`MyApplication.kt`):
- Initializes Firebase, Vosk speech recognition, and notification channels
- Extracts Vosk model files from assets to internal storage
- Creates notification channels for voice service, emergency alerts, and chat messages

**Service Layer** (`VoiceRecognitionService.kt`):
- Foreground service that runs voice recognition in background
- Uses AudioRecord for continuous audio capture
- Integrates Vosk for offline speech recognition
- Triggers media recording based on voice commands
- Automatically uploads recordings to Firebase Storage

**Repository Layer** (`FirebaseRepository.kt`):
- Manages all Firebase operations (Auth, Database, Storage)
- Handles proximity-based chat creation and management
- Manages user location and chat participant operations

**Data Models**:
- `ChatMessage`: Message structure with multimedia support
- `ProximityChat`: Location-based chat rooms
- `User`: User profile and authentication data

### UI Architecture
- **Navigation**: Single activity with Navigation Compose
- **Screens**: Splash → Login/Auth → Main → Chat
- **Components**: Reusable UI components in `presentation/components/`
- **Theme**: Material 3 design system with custom theming

### Background Processing
- **VoiceRecognitionService**: Maintains wake lock, handles voice commands
- **WorkManager**: Handles background tasks and job scheduling
- **Notification Management**: Three channels for different notification types

## Key Dependencies

- **Vosk (0.3.47)**: Offline speech recognition with Spanish model
- **Firebase**: Complete backend solution (Auth, Database, Storage, Analytics)
- **CameraX**: Video recording functionality
- **Media3**: Audio/video playback
- **Hilt**: Dependency injection
- **Jetpack Compose**: UI framework
- **Coroutines**: Asynchronous programming
- **Accompanist**: Compose utilities (permissions)

## Development Notes

### Voice Recognition Setup
- Vosk model files are extracted from `assets/vosk-model/` to internal storage
- Service requires RECORD_AUDIO, FOREGROUND_SERVICE, and WAKE_LOCK permissions
- Voice commands trigger automatic recording and Firebase upload

### Firebase Configuration
- Requires `google-services.json` in app directory
- Database structure: `users/`, `proximity_chats/`, `chat_messages/`, `chat_participants/`
- Storage is used for multimedia file uploads (audio/video recordings)

### Permissions Required
- RECORD_AUDIO, CAMERA: Media recording
- FOREGROUND_SERVICE, WAKE_LOCK: Background service operation
- ACCESS_FINE_LOCATION: Proximity-based features
- WRITE_EXTERNAL_STORAGE: File operations

### NDK Configuration
- Supports ARM64, ARMv7, x86, x86_64 architectures for Vosk native libraries
- JNA library required for Vosk integration
- Legacy packaging enabled for native libraries