# SafeVoice Chat - Proximity-Based Voice-Controlled Communication System

A sophisticated Android chat application with advanced voice recognition capabilities, location-based proximity chats, and real-time communication features powered by Firebase.

---

## 📱 Overview

**SafeVoice Chat** (DemoAppChat) is a professional-grade Android application designed for secure, location-aware group communication. The app features:

- 🎤 **Advanced Voice Recognition** using Vosk offline speech recognition
- 📍 **Proximity-Based Chat** creation and discovery
- 🔐 **Secure Authentication** with Firebase Auth
- 💬 **Real-time Messaging** with Firebase Realtime Database
- 🎥 **Video & Audio Calls** with WebRTC integration
- 📱 **Multi-Device Compatibility** with device-specific optimizations
- 🌐 **Offline Capability** with offline voice recognition
- ⚡ **Background Services** for 24/7 voice command monitoring

---

## 🎯 Key Features

### Voice Recognition System
- **Offline Speech Recognition** using Vosk models
- **Automatic Voice Commands** for hands-free operation
- **Custom Command Configuration** with configurable sensitivity
- **Multi-Engine Fallback** system for enhanced accuracy
- **Pattern-Based Detection** for low-confidence scenarios

### Proximity Chat System
- **Location-Based Chat Creation** with customizable radius
- **Automatic User Discovery** within proximity
- **PIN-Protected Groups** for security
- **Real-time Participant Tracking**
- **Automatic Chat Expiration** management

### Media & Communication
- **Text Messaging** with rich media support
- **Voice Messages** with automatic recording
- **Video Messages** and recording
- **Audio/Video Calls** using WebRTC
- **Group Video Calls** support
- **File Sharing** through Firebase Storage

### Device Compatibility
- **Universal Compatibility** with all Android devices (API 24+)
- **Manufacturer-Specific Optimizations** for:
  - Honor/Huawei devices
  - Xiaomi (MIUI) devices  
  - Oppo (ColorOS) devices
  - Vivo (FuntouchOS) devices
  - Samsung devices
  - OnePlus devices
  - Generic Android devices

---

## 🛠️ Technology Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt (Dagger 2)
- **Async Processing**: Kotlin Coroutines & Flow

### Backend & Services
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Storage**: Firebase Cloud Storage
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Analytics**: Firebase Analytics

### Media & Communication
- **Voice Recognition**: Vosk (offline speech recognition)
- **Audio Recording**: MediaRecorder API
- **Video**: CameraX + ExoPlayer
- **WebRTC**: WebRTC for peer-to-peer calls
- **Location**: Google Play Services Location

### Libraries & Dependencies
```kotlin
// Core
androidx.core:core-ktx:1.16.0
androidx.lifecycle:lifecycle-runtime-ktx:2.8.7
androidx.activity:activity-compose:1.10.1

// Compose
androidx.compose:compose-bom:2024.10.01
androidx.compose.material3:material3
androidx.navigation:navigation-compose:2.7.5

// Firebase
com.google.firebase:firebase-bom:32.7.0
firebase-auth-ktx
firebase-database-ktx
firebase-storage-ktx
firebase-messaging-ktx

// Hilt
com.google.dagger:hilt-android:2.48
androidx.hilt:hilt-navigation-compose:1.1.0

// Voice Recognition
com.alphacephei:vosk-android:0.3.47

// Camera & Video
androidx.camera:camera-camera2:1.3.1
androidx.media3:media3-exoplayer:1.2.1

// WebRTC
io.pristine:libjingle:11139
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API 24 (Android 7.0) or higher
- JDK 8 or higher
- Firebase project with:
  - Authentication enabled
  - Realtime Database configured
  - Cloud Storage enabled
  - FCM enabled

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/demoappchat.git
   cd demoappchat
   ```

2. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Download `google-services.json`
   - Place it in `app/` directory
   - Update Firebase rules using `firebase_rules.json`

3. **Configure Vosk Model**
   - The app automatically downloads the Spanish Vosk model on first run
   - Or manually download from [Vosk Models](https://alphacephei.com/vosk/models)
   - Place in `app/src/main/assets/vosk-model/`

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

---

## 📋 Configuration

### Firebase Setup

#### 1. Authentication
Enable the following sign-in methods:
- Email/Password
- Google Sign-In

#### 2. Realtime Database Rules
```json
{
  "rules": {
    "users": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$uid": {
        ".write": "$uid === auth.uid"
      }
    },
    "proximity_chats": {
      ".read": "auth != null",
      ".write": "auth != null",
      ".indexOn": ["latitude", "longitude", "createdAt", "isActive"]
    },
    "chat_messages": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null",
        ".indexOn": ["timestamp"]
      }
    },
    "chat_participants": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

#### 3. Storage Rules
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /chat_media/{chatId}/{fileName} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Device-Specific Optimizations

The app automatically detects your device and applies appropriate optimizations:

#### For Honor/Huawei Devices:
- Extended check intervals (3000ms)
- Longer recognition timeouts (10000ms)
- Lower confidence threshold (60%)
- Automatic battery optimization prompts

#### For Xiaomi Devices:
- MIUI-specific battery optimizations
- Autostart permission prompts
- Power saving mode handling

#### For Oppo/Vivo Devices:
- ColorOS/FuntouchOS optimizations
- App protection settings
- Background restriction handling

---

## 🎮 Usage

### Basic Setup

1. **First Launch**
   - Grant required permissions (Microphone, Location, Camera)
   - Allow battery optimization exemption
   - Complete device-specific setup

2. **Authentication**
   - Sign in with email/password or Google
   - Profile is automatically created

3. **Create Location**
   - Grant location permissions
   - Your location is used for proximity chat discovery

### Voice Commands

The app supports automatic voice commands for hands-free operation:

#### Emergency Commands (5km radius)
- "emergencia" - Creates emergency chat
- "ayuda" - Creates help chat
- "socorro" - Creates SOS chat

#### Alert Commands (3km radius)
- "alerta" - Creates alert chat

#### Surveillance Commands (4km radius)
- "vigilancia" - Creates surveillance chat
- "observar" - Creates observation chat
- "monitorear" - Creates monitoring chat

#### Recording Commands (2km radius)
- "grabar" - Creates recording chat
- "audio" - Creates audio chat
- "sonido" - Creates sound chat

#### General Commands (3km radius)
- "chat grupal" - Creates general group chat
- "grupo" - Creates group chat
- "conversar" - Creates conversation chat

### Creating Proximity Chats

1. **Manual Creation**
   - Tap "Create Chat" button
   - Enter chat name and description
   - Set radius (500m - 10km)
   - Set PIN (4 digits)
   - Select category

2. **Voice Creation**
   - Say any voice command
   - Chat is automatically created
   - You're automatically registered
   - Audio recording starts automatically
   - Nearby users are notified

### Joining Chats

1. **Automatic Discovery**
   - Nearby chats appear automatically
   - Based on your GPS location

2. **Manual Join**
   - Select chat from list
   - Enter PIN
   - Join and start communicating

---

## 📱 App Architecture

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/demoappchat/
│   │   ├── data/                    # Data layer
│   │   │   ├── model/              # Data models
│   │   │   ├── repository/         # Repositories
│   │   │   ├── service/            # Services
│   │   │   │   ├── voice/          # Voice recognition engines
│   │   │   │   ├── VoiceRecognitionService.kt
│   │   │   │   ├── BackgroundVoiceService.kt
│   │   │   │   ├── SimpleMediaRecordingService.kt
│   │   │   │   └── ErrorLogger.kt
│   │   │   ├── receiver/           # Broadcast receivers
│   │   │   └── webrtc/             # WebRTC implementation
│   │   ├── domain/                 # Domain layer
│   │   │   ├── model/              # Domain models
│   │   │   └── usecase/            # Use cases
│   │   ├── presentation/           # Presentation layer
│   │   │   ├── auth/               # Authentication screens
│   │   │   ├── main/               # Main screen
│   │   │   ├── chat/               # Chat screens
│   │   │   ├── settings/           # Settings screens
│   │   │   └── components/         # Reusable components
│   │   ├── di/                     # Dependency injection
│   │   ├── ui/theme/               # Theme and styling
│   │   ├── utils/                  # Utilities
│   │   │   ├── DeviceCompatibilityManager.kt
│   │   │   └── NavigationHelper.kt
│   │   ├── MainActivity.kt
│   │   └── MyApplication.kt
│   ├── res/                        # Resources
│   └── AndroidManifest.xml
└── build.gradle.kts
```

### Architecture Pattern

The app follows **Clean Architecture** principles:

```
┌─────────────────────────────────────┐
│      Presentation Layer             │
│  (ViewModels, Compose UI, Activity) │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│       Domain Layer                  │
│     (Use Cases, Models)             │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│         Data Layer                  │
│  (Repositories, Services, Firebase) │
└─────────────────────────────────────┘
```

---

## 🔧 Advanced Configuration

### Voice Recognition Settings

Adjust voice recognition in Settings:
- **Sensitivity**: 0-100% (default: 70%)
- **Recording Duration**: 1-60 seconds
- **Auto-recording**: Enable/disable
- **Stealth Mode**: Hidden recording
- **24/7 Mode**: Always-on listening

### Notification Channels

The app uses multiple notification channels:
- **Voice Recognition** (High priority)
- **Emergency Alerts** (Urgent)
- **Chat Messages** (Default)
- **Background Service** (Low priority)

---

## 🐛 Troubleshooting

### Voice Commands Not Working

**Problem**: Voice commands aren't detected

**Solutions**:
1. Check microphone permissions
2. Verify Vosk model is installed
3. Adjust sensitivity in Settings
4. Check device-specific optimizations
5. Restart voice service

### Chats Not Appearing

**Problem**: Nearby chats don't show up

**Solutions**:
1. Enable location services
2. Grant location permissions
3. Check GPS signal
4. Verify Firebase connection
5. Check chat radius settings

### App Stops in Background (Honor/Xiaomi/Oppo)

**Problem**: App stops working when screen is off

**Solutions**:
1. Disable battery optimization
2. Enable autostart permission
3. Add app to protected apps
4. Configure power management
5. Enable background restrictions exemption

### Messages Not Sending

**Problem**: Messages fail to send

**Solutions**:
1. Check internet connection
2. Verify Firebase authentication
3. Check Firebase database rules
4. Verify you're a chat participant
5. Check Firebase quota limits

---

## 🔐 Security & Privacy

### Data Protection
- All communications encrypted in transit (TLS)
- Firebase security rules enforce access control
- PIN-protected group chats
- Location data only shared within chat radius

### Permissions
- **Microphone**: Voice commands and audio messages
- **Camera**: Video messages and calls
- **Location**: Proximity chat discovery
- **Storage**: Media file caching
- **Notifications**: Chat and alert notifications

### Best Practices
- Don't share chat PINs publicly
- Review location permissions regularly
- Use strong passwords for authentication
- Keep app updated for security patches

---

## 📊 Performance

### Optimization Strategies
- Lazy initialization of services
- Efficient Firebase listeners
- Image compression for media
- Background task management
- Memory leak prevention

### Battery Optimization
- Doze mode compatibility
- JobScheduler for deferred tasks
- Wake lock management
- Efficient location updates

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add KDoc comments for public APIs
- Keep functions small and focused

---

## 📝 Version History

### Version 1.0.0 (Current)
- Initial release
- Voice recognition with Vosk
- Proximity-based chats
- WebRTC video calls
- Multi-device compatibility
- Firebase integration

---

## 🆘 Support

For issues, questions, or feature requests:
- Open an issue on GitHub
- Check documentation
- Review Firebase console for backend issues

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## 🙏 Acknowledgments

- **Vosk** - Offline speech recognition
- **Firebase** - Backend infrastructure
- **WebRTC** - Real-time communication
- **Jetpack Compose** - Modern UI framework
- **Android Community** - Support and resources

---

## 📞 Contact

- **Project**: DemoAppChat (SafeVoice)
- **Platform**: Android (API 24+)
- **Status**: Active Development

---

**Built with ❤️ for secure, hands-free communication**

