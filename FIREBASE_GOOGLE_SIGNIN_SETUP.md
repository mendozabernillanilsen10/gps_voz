# Configuración de Google Sign-In en Firebase

## Pasos para habilitar Google Sign-In:

### 1. Firebase Console
1. Ve a https://console.firebase.google.com
2. Selecciona tu proyecto: `demoappchat-d7407`
3. Ve a **image.png* en el menú lateral
4. Haz clic en **Sign-in method**
5. Busca **Google** en la lista de proveedores
6. Haz clic en **Google** y luego en **Enable**
7. Configura el **Project support email**
8. Haz clic en **Save**

### 2. Google Cloud Console
1. Ve a https://console.cloud.google.com
2. Selecciona tu proyecto: `demoappchat-d7407`
3. Ve a **APIs & Services** > **OAuth consent screen**
4. Configura la pantalla de consentimiento:
   - **User Type**: External
   - **App name**: SafeVoice
   - **User support email**: Tu email
   - **Developer contact information**: Tu email
5. Haz clic en **Save and Continue**
6. En **Scopes**, agrega:
   - `email`
   - `profile`
   - `openid`
7. Haz clic en **Save and Continue**
8. En **Test users**, agrega tu email
9. Haz clic en **Save and Continue**

### 3. Obtener Web Client ID
1. Ve a **APIs & Services** > **Credentials**
2. Busca la sección **OAuth 2.0 Client IDs**
3. Copia el **Web client ID** (termina en `.apps.googleusercontent.com`)

### 4. Actualizar el código
Una vez que tengas el Web Client ID, actualiza el archivo `GoogleSignInHelper.kt`:

```kotlin
private fun setupGoogleSignIn() {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("TU_WEB_CLIENT_ID_AQUI") // Reemplazar con el ID real
        .requestEmail()
        .build()
    
    googleSignInClient = GoogleSignIn.getClient(context, gso)
}
```

### 5. Verificar configuración
- Asegúrate de que tu app tenga la huella digital SHA-1 configurada en Firebase
- Para obtener la huella digital SHA-1 de debug:
  ```bash
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

### 6. Probar
Una vez configurado, el botón "Continuar con Google" debería funcionar correctamente.

## Notas importantes:
- El Web Client ID es necesario para que Firebase Auth funcione correctamente con Google
- Sin el Web Client ID, el login funcionará pero no se integrará con Firebase Auth
- Asegúrate de que tu app esté en modo de prueba en Google Cloud Console
