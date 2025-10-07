# Setup Guide - SafeVoice Chat Development

Complete step-by-step guide for setting up the development environment.

---

## 📋 Prerequisites

### Required Software
- **Android Studio**: Hedgehog (2023.1.1) or later
  - Download from: https://developer.android.com/studio
- **JDK**: Java Development Kit 8 or higher
- **Git**: Version control
  - Download from: https://git-scm.com/
- **Firebase CLI** (optional, for database rules):
  ```bash
  npm install -g firebase-tools
  ```

### Required Accounts
- **Firebase Account**: https://console.firebase.google.com
- **Google Cloud Console**: https://console.cloud.google.com (for SHA-1 fingerprint)

---

## 🔧 Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/demoappchat.git
cd demoappchat
```

### 2. Firebase Project Setup

#### A. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter project name: `safevoice-chat` (or your choice)
4. Enable Google Analytics (recommended)
5. Click "Create project"

#### B. Add Android App
1. Click "Add app" → Android icon
2. Enter package name: `com.example.demoappchat`
3. Enter app nickname: "SafeVoice Chat"
4. Download `google-services.json`
5. Place file in `app/` directory

#### C. Get SHA-1 Fingerprint

**Debug Certificate**:
```powershell
# Windows (PowerShell)
.\get_sha1_fingerprint.ps1

# Or manually:
cd android
.\gradlew signingReport
```

**Linux/Mac**:
```bash
./gradlew signingReport
```

Copy the SHA-1 from debug keystore and add to Firebase:
- Firebase Console → Project Settings → Your apps → Add fingerprint

#### D. Enable Firebase Services

**Authentication**:
1. Firebase Console → Authentication → Get started
2. Sign-in method → Email/Password → Enable
3. Sign-in method → Google → Enable
   - Enter support email
   - Save

**Realtime Database**:
1. Firebase Console → Realtime Database → Create database
2. Start in **test mode** (we'll update rules later)
3. Choose database location (closest to your users)

**Storage**:
1. Firebase Console → Storage → Get started
2. Start in **test mode**
3. Choose storage location

**Cloud Messaging**:
1. Firebase Console → Cloud Messaging
2. Automatically enabled with google-services.json

### 3. Apply Firebase Rules

#### Option A: Using Firebase CLI (Recommended)
```powershell
# Login to Firebase
firebase login

# Deploy rules
firebase deploy --only database
```

#### Option B: Manual Setup
Copy rules from `firebase_rules.json` to:
- Firebase Console → Realtime Database → Rules

### 4. Configure Android Studio

#### A. Open Project
1. Open Android Studio
2. File → Open → Select `demoappchat` folder
3. Wait for Gradle sync

#### B. Configure SDK
1. File → Project Structure
2. SDK Location → Ensure Android SDK is set
3. Project → Gradle Version: 8.7.3
4. Project → Kotlin Version: 2.0.0

#### C. Install Required SDKs
Tools → SDK Manager:
- ✅ Android 14.0 (API 34) - Compilesdk
- ✅ Android 13.0 (API 33) - Targetsdk
- ✅ Android 7.0 (API 24) - Minsdk
- ✅ Android SDK Build-Tools 34
- ✅ Android SDK Platform-Tools
- ✅ Android SDK Tools

#### D. Install Required Plugins
File → Settings → Plugins:
- ✅ Kotlin
- ✅ Android
- ✅ Gradle

### 5. Download Vosk Model

The app automatically downloads the model on first run, but you can pre-download:

```powershell
# Automatic download (provided script)
.\download_vosk_model.ps1
```

**Manual Download**:
1. Download Spanish model: https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip
2. Extract to `app/src/main/assets/vosk-model/`
3. Ensure structure:
   ```
   app/src/main/assets/vosk-model/
   ├── am/
   ├── conf/
   ├── graph/
   └── ivector/
   ```

### 6. Build Configuration

#### A. Gradle Sync
1. File → Sync Project with Gradle Files
2. Wait for sync to complete
3. Fix any dependency issues

#### B. Build Variants
1. Build → Select Build Variant
2. Choose `debug` for development
3. Choose `release` for production

### 7. Run the App

#### A. Using Emulator
1. Tools → Device Manager
2. Create Device → Phone → Pixel 6
3. System Image → API 34 (Android 14)
4. Click Play button to run

#### B. Using Physical Device
1. Enable Developer Options:
   - Settings → About phone → Tap "Build number" 7 times
2. Enable USB Debugging:
   - Settings → Developer options → USB debugging
3. Connect device via USB
4. Click Play button to run

---

## 🧪 Testing Setup

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Login with email/password
- [ ] Login with Google
- [ ] Create proximity chat
- [ ] Join proximity chat
- [ ] Send text message
- [ ] Send voice message
- [ ] Voice command recognition
- [ ] Video call
- [ ] Location permissions
- [ ] Battery optimization

---

## 🔐 Security Setup

### 1. Firebase Security Rules

**Realtime Database**:
```json
{
  "rules": {
    "users": {
      ".read": "auth != null",
      "$uid": {
        ".write": "$uid === auth.uid"
      }
    },
    "proximity_chats": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "chat_messages": {
      "$chatId": {
        ".read": "root.child('chat_participants').child($chatId).child(auth.uid).exists()",
        ".write": "root.child('chat_participants').child($chatId).child(auth.uid).exists()"
      }
    }
  }
}
```

**Storage Rules**:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /chat_media/{chatId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
                   && request.resource.size < 10 * 1024 * 1024;
    }
  }
}
```

### 2. API Keys Protection

Never commit:
- `google-services.json` (add to .gitignore if needed)
- Release keystore files
- API keys in code

Use:
- BuildConfig for API keys
- Environment variables
- Firebase Remote Config

---

## 📱 Device-Specific Testing

### Honor/Huawei Devices
1. Settings → Battery → App launch → Your app → Manage manually
2. Enable all permissions
3. Settings → Apps → Your app → Autostart → Enable
4. Test background service persistence

### Xiaomi Devices
1. Security → Permissions → Autostart → Enable for app
2. Settings → Battery & performance → Choose apps → Your app → No restrictions
3. Settings → Additional settings → Privacy → Special permissions → Display pop-up windows while running in background

### Oppo/Vivo Devices
1. Settings → Battery → App battery management → Your app → Don't optimize
2. Settings → Security → Startup manager → Your app → Enable
3. Settings → Apps → Your app → Autostart → Enable

---

## 🐛 Common Setup Issues

### Issue: Gradle Sync Failed

**Solution**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

### Issue: Firebase google-services.json not found

**Solution**:
- Ensure `google-services.json` is in `app/` directory
- Check file name (must be exact)
- Sync Gradle files again

### Issue: Vosk Model Not Loading

**Solution**:
- Check `app/src/main/assets/vosk-model/` exists
- Verify all model files present
- Check logcat for specific errors
- Re-download model

### Issue: Location Not Working

**Solution**:
- Check Google Play Services installed
- Grant location permissions
- Enable GPS
- Test on physical device (emulator GPS is limited)

### Issue: Voice Recognition Not Working

**Solution**:
- Check microphone permissions
- Test on physical device (emulator mic is limited)
- Verify Vosk model loaded
- Check sensitivity settings
- Review device-specific optimizations

---

## 📊 Development Tools

### Useful ADB Commands

```bash
# View logs in real-time
adb logcat | grep "VoiceService\|MainActivity\|FirebaseRepo"

# Clear app data
adb shell pm clear com.example.demoappchat

# Grant permissions
adb shell pm grant com.example.demoappchat android.permission.RECORD_AUDIO
adb shell pm grant com.example.demoappchat android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.demoappchat android.permission.CAMERA

# Check running services
adb shell dumpsys activity services | grep "demoappchat"

# Monitor battery stats
adb shell dumpsys battery
```

### Firebase Debugging

```bash
# Enable Firebase debug logging
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE

# View Firebase logs
adb logcat | grep "Firebase"
```

---

## 🚀 Next Steps

After setup is complete:

1. **Review Architecture**: Read `README.md` architecture section
2. **Explore Code**: Start with `MainActivity.kt` and `MainViewModel.kt`
3. **Run Tests**: Execute unit and instrumented tests
4. **Check Documentation**: Review inline KDoc comments
5. **Join Development**: Read `CONTRIBUTING.md`

---

## 📞 Support

If you encounter issues:
- Check this guide thoroughly
- Review Firebase Console for backend issues
- Check device-specific compatibility
- Open GitHub issue with details

---

**Happy Coding! 🎉**

