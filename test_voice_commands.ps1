# Script de prueba para el sistema de comandos de voz automáticos
# Ejecutar después de compilar e instalar la aplicación

Write-Host "🎤 Probando Sistema de Comandos de Voz Automáticos" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Verificar que la aplicación esté instalada
Write-Host "1. Verificando instalación de la aplicación..." -ForegroundColor Yellow
$packageName = "com.example.demoappchat"
$isInstalled = adb shell pm list packages | Select-String $packageName

if ($isInstalled) {
    Write-Host "✅ Aplicación instalada correctamente" -ForegroundColor Green
} else {
    Write-Host "❌ Aplicación no encontrada. Instalando..." -ForegroundColor Red
    # Aquí iría el comando de instalación si fuera necesario
    exit 1
}

# Verificar permisos
Write-Host "`n2. Verificando permisos..." -ForegroundColor Yellow
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
Start-Sleep -Seconds 3

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

# Verificar logs de inicialización
Write-Host "`n5. Verificando logs de inicialización..." -ForegroundColor Yellow
Write-Host "Buscando logs de servicios de voz..." -ForegroundColor Cyan

# Comando para ver logs en tiempo real
Write-Host "`n📋 Comandos para monitorear logs:" -ForegroundColor Magenta
Write-Host "adb logcat | grep -E '(VoiceService|BackgroundVoiceService|FirebaseRepo)'" -ForegroundColor White
Write-Host "adb logcat | grep -E '(MainActivity|VoiceCommandReceiver)'" -ForegroundColor White

# Instrucciones de prueba
Write-Host "`n🎯 Instrucciones para probar comandos de voz:" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

Write-Host "`n📝 Comandos de prueba disponibles:" -ForegroundColor Cyan
Write-Host "• 'emergencia' - Crea chat de emergencia (5km)" -ForegroundColor White
Write-Host "• 'ayuda' - Crea chat de emergencia (5km)" -ForegroundColor White
Write-Host "• 'socorro' - Crea chat de emergencia (5km)" -ForegroundColor White
Write-Host "• 'alerta' - Crea chat de alerta (3km)" -ForegroundColor White
Write-Host "• 'vigilancia' - Crea chat de vigilancia (4km)" -ForegroundColor White
Write-Host "• 'observar' - Crea chat de vigilancia (4km)" -ForegroundColor White
Write-Host "• 'monitorear' - Crea chat de vigilancia (4km)" -ForegroundColor White
Write-Host "• 'grabar' - Crea chat de grabación (2km)" -ForegroundColor White
Write-Host "• 'audio' - Crea chat de grabación (2km)" -ForegroundColor White
Write-Host "• 'sonido' - Crea chat de grabación (2km)" -ForegroundColor White
Write-Host "• 'chat grupal' - Crea chat general (3km)" -ForegroundColor White
Write-Host "• 'grupo' - Crea chat general (3km)" -ForegroundColor White
Write-Host "• 'conversar' - Crea chat general (3km)" -ForegroundColor White

Write-Host "`n🔍 Pasos para verificar funcionamiento:" -ForegroundColor Cyan
Write-Host "1. Asegúrate de estar autenticado en la aplicación" -ForegroundColor White
Write-Host "2. Di uno de los comandos de voz en voz alta y clara" -ForegroundColor White
Write-Host "3. Verifica en los logs que se detecte el comando" -ForegroundColor White
Write-Host "4. Verifica que se cree el chat grupal automáticamente" -ForegroundColor White
Write-Host "5. Verifica que comience la grabación de audio" -ForegroundColor White
Write-Host "6. Verifica que se envíen notificaciones a usuarios cercanos" -ForegroundColor White

Write-Host "`n📊 Logs esperados:" -ForegroundColor Cyan
Write-Host "✅ '🎯 Comando detectado: [comando]'" -ForegroundColor Green
Write-Host "✅ '🏗️ Creando chat grupal automático: [acción]'" -ForegroundColor Green
Write-Host "✅ '✅ Chat grupal creado: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '👤 Usuario registrado en chat: [chat_id]'" -ForegroundColor Green
Write-Host "✅ '🎤 Iniciando grabación automática'" -ForegroundColor Green
Write-Host "✅ '📢 Notificando a usuarios cercanos'" -ForegroundColor Green

Write-Host "`n⚠️  Notas importantes:" -ForegroundColor Yellow
Write-Host "• El sistema funciona 24/7 una vez inicializado" -ForegroundColor White
Write-Host "• Los servicios se reinician automáticamente si se detienen" -ForegroundColor White
Write-Host "• Las notificaciones se envían según el radio configurado" -ForegroundColor White
Write-Host "• La grabación de audio es automática al crear el chat" -ForegroundColor White

Write-Host "`n🚀 Sistema listo para pruebas!" -ForegroundColor Green
Write-Host "Ejecuta los comandos de voz y monitorea los logs para verificar el funcionamiento." -ForegroundColor Cyan
