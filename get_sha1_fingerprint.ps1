# Script para obtener la huella digital SHA-1 del keystore de debug
# Esto es necesario para configurar Google Sign-In en Firebase

Write-Host "🔍 Obteniendo huella digital SHA-1 del keystore de debug..." -ForegroundColor Cyan

# Ruta del keystore de debug (Windows)
$debugKeystorePath = "$env:USERPROFILE\.android\debug.keystore"

# Verificar si existe el keystore
if (Test-Path $debugKeystorePath) {
    Write-Host "✅ Keystore encontrado en: $debugKeystorePath" -ForegroundColor Green
    
    # Comando para obtener SHA-1
    $command = "keytool -list -v -keystore `"$debugKeystorePath`" -alias androiddebugkey -storepass android -keypass android"
    
    Write-Host "🔧 Ejecutando comando..." -ForegroundColor Yellow
    Write-Host "Comando: $command" -ForegroundColor Gray
    
    try {
        $result = Invoke-Expression $command
        
        # Buscar la línea que contiene SHA1
        $sha1Line = $result | Where-Object { $_ -match "SHA1:" }
        
        if ($sha1Line) {
            Write-Host "`n🎯 Huella digital SHA-1 encontrada:" -ForegroundColor Green
            Write-Host $sha1Line -ForegroundColor White
            Write-Host "`n📋 Copia esta línea y agrégalo a Firebase Console:" -ForegroundColor Cyan
            Write-Host "1. Ve a Firebase Console > Project Settings" -ForegroundColor Yellow
            Write-Host "2. En la sección 'Your apps', selecciona tu app Android" -ForegroundColor Yellow
            Write-Host "3. Haz clic en 'Add fingerprint'" -ForegroundColor Yellow
            Write-Host "4. Pega la huella digital SHA-1" -ForegroundColor Yellow
        } else {
            Write-Host "❌ No se pudo encontrar la huella digital SHA-1 en la salida" -ForegroundColor Red
            Write-Host "Salida completa:" -ForegroundColor Gray
            $result
        }
    }
    catch {
        Write-Host "❌ Error ejecutando keytool: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "`n💡 Asegúrate de que Java esté instalado y en el PATH" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Keystore de debug no encontrado en: $debugKeystorePath" -ForegroundColor Red
    Write-Host "`n💡 Esto puede suceder si:" -ForegroundColor Yellow
    Write-Host "   - No has ejecutado la app en modo debug" -ForegroundColor Yellow
    Write-Host "   - El keystore está en una ubicación diferente" -ForegroundColor Yellow
    Write-Host "`n🔧 Para crear el keystore de debug, ejecuta la app desde Android Studio" -ForegroundColor Cyan
}

Write-Host "`n📚 Para más información sobre Google Sign-In:" -ForegroundColor Cyan
Write-Host "   - Revisa el archivo FIREBASE_GOOGLE_SIGNIN_SETUP.md" -ForegroundColor Yellow
