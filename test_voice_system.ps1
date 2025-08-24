# Script de prueba para verificar el registro automático de usuarios en chats grupales
# Ejecutar después de compilar e instalar la aplicación

Write-Host "🎤 Probando Sistema de Registro Automático en Chats Grupales" -ForegroundColor Green
Write-Host "=============================================================" -ForegroundColor Green

# Verificar que la aplicación esté instalada
Write-Host "1. Verificando instalación de la aplicación..." -ForegroundColor Yellow
$packageName = "com.example.demoappchat"
$isInstalled = adb shell pm list packages | Select-String $packageName

if ($isInstalled) {
    Write-Host "✅ Aplicación instalada correctamente" -ForegroundColor Green
} else {
    Write-Host "❌ Aplicación no encontrada. Instalando..." -ForegroundColor Red
    exit 1
}

# Verificar permisos
Write-Host "`n2. Verificando permisos necesarios..." -ForegroundColor Yellow
$permissions = @(
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.WAKE_LOCK"
)

foreach ($permission in $permissions) {
    $granted = adb shell dumpsys package $packageName | Select-String $permission
    if ($granted) {
        Write-Host "✅ $permission - Concedido" -ForegroundColor Green
    } else {
        Write-Host "❌ $permission - No concedido" -ForegroundColor Red
    }
}

# Iniciar aplicación
Write-Host "`n3. Iniciando aplicación..." -ForegroundColor Yellow
adb shell am start -n "$packageName/.MainActivity"
Start-Sleep -Seconds 5

# Verificar servicios activos
Write-Host "`n4. Verificando servicios activos..." -ForegroundColor Yellow
$services = @(
    "VoiceRecognitionService",
    "BackgroundVoiceService"
)

foreach ($service in $services) {
    $isRunning = adb shell dumpsys activity services | Select-String $service
    if ($isRunning) {
        Write-Host "✅ $service - Activo" -ForegroundColor Green
    } else {
        Write-Host "❌ $service - No activo" -ForegroundColor Red
    }
}

# Comandos para monitorear logs
Write-Host "`n📋 Comandos para monitorear logs en tiempo real:" -ForegroundColor Magenta
Write-Host "adb logcat | grep -E '(VoiceService|MainActivity)'" -ForegroundColor White
Write-Host "adb logcat | grep -E '(FirebaseRepo|BackgroundVoiceService)'" -ForegroundColor White

# Instrucciones específicas para el registro automático
Write-Host "`n🎯 Instrucciones para probar registro automático:" -ForegroundColor Magenta
Write-Host "=================================================" -ForegroundColor Magenta

Write-Host "`n📝 Pasos para verificar registro automático:" -ForegroundColor Cyan
Write-Host "1. Asegúrate de estar autenticado en la aplicación" -ForegroundColor White
Write-Host "2. Di uno de estos comandos de voz:" -ForegroundColor White
Write-Host "   • 'emergencia' - Crea chat de emergencia" -ForegroundColor Yellow
Write-Host "   • 'alerta' - Crea chat de alerta" -ForegroundColor Yellow
Write-Host "   • 'vigilancia' - Crea chat de vigilancia" -ForegroundColor Yellow
Write-Host "   • 'grabar' - Crea chat de grabación" -ForegroundColor Yellow
Write-Host "   • 'chat grupal' - Crea chat general" -ForegroundColor Yellow

Write-Host "`n🔍 Logs esperados para registro automático:" -ForegroundColor Cyan
Write-Host "✅ '🎯 Comando detectado: [comando]'" -ForegroundColor Green
Write-Host "✅ '🏗️ Iniciando creación de chat grupal: [título]'" -ForegroundColor Green
Write-Host "✅ '🔐 Usuario creador: [uid] - [nombre]'" -ForegroundColor Green
Write-Host "✅ '📝 Datos del chat a crear: [datos]'" -ForegroundColor Green
Write-Host "✅ '✅ Chat grupal creado exitosamente en Firebase: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '👤 Iniciando registro de usuario en chat: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '🔐 Usuario autenticado: [uid] - [nombre]'" -ForegroundColor Green
Write-Host "✅ '📝 Datos del participante: [datos]'" -ForegroundColor Green
Write-Host "✅ '✅ Usuario registrado exitosamente en chat: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '🎤 Iniciando grabación automática en chat: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '📢 Notificando a usuarios cercanos...'" -ForegroundColor Green

Write-Host "`n🔧 Verificación en Firebase Database:" -ForegroundColor Cyan
Write-Host "1. Ir a Firebase Console > Realtime Database" -ForegroundColor White
Write-Host "2. Verificar en 'proximity_chats/[chat_id]':" -ForegroundColor White
Write-Host "   • creatorId debe coincidir con el usuario autenticado" -ForegroundColor Yellow
Write-Host "   • participantsCount debe ser 1" -ForegroundColor Yellow
Write-Host "   • createdByVoice debe ser true" -ForegroundColor Yellow
Write-Host "3. Verificar en 'chat_participants/[chat_id]/[user_id]':" -ForegroundColor White
Write-Host "   • userId debe coincidir con el usuario autenticado" -ForegroundColor Yellow
Write-Host "   • isCreator debe ser true" -ForegroundColor Yellow
Write-Host "   • joinedByVoice debe ser true" -ForegroundColor Yellow
Write-Host "   • autoJoined debe ser true" -ForegroundColor Yellow

Write-Host "`n⚠️  Problemas comunes y soluciones:" -ForegroundColor Yellow
Write-Host "• Si no se detecta el comando: Verificar permisos de micrófono" -ForegroundColor White
Write-Host "• Si no se crea el chat: Verificar conexión a Firebase" -ForegroundColor White
Write-Host "• Si no se registra el usuario: Verificar autenticación" -ForegroundColor White
Write-Host "• Si no hay logs: Verificar que los servicios estén activos" -ForegroundColor White

Write-Host "`n🚀 Sistema listo para pruebas!" -ForegroundColor Green
Write-Host "Ejecuta un comando de voz y verifica los logs para confirmar el registro automático." -ForegroundColor Cyan

Write-Host "`n📊 Comando para ver logs específicos de registro:" -ForegroundColor Magenta
Write-Host "adb logcat | grep -E '(Usuario registrado|Chat grupal creado|Usuario autenticado)'" -ForegroundColor White

