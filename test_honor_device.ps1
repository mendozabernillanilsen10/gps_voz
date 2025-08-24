# Test Script para Honor X6b Plus
# Este script verifica y configura la aplicación específicamente para Honor X6b Plus

Write-Host "🚀 Iniciando configuración para Honor X6b Plus..." -ForegroundColor Green

# 1. Verificar que el dispositivo esté conectado
Write-Host "📱 Verificando conexión del dispositivo..." -ForegroundColor Yellow
$devices = adb devices
if ($devices -match "device") {
    Write-Host "✅ Dispositivo conectado" -ForegroundColor Green
} else {
    Write-Host "❌ No hay dispositivos conectados. Conecta tu Honor X6b Plus." -ForegroundColor Red
    exit 1
}

# 2. Compilar e instalar la aplicación
Write-Host "🔧 Compilando e instalando aplicación..." -ForegroundColor Yellow
./gradlew assembleDebug
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilación exitosa" -ForegroundColor Green
} else {
    Write-Host "❌ Error en compilación" -ForegroundColor Red
    exit 1
}

./gradlew installDebug
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Instalación exitosa" -ForegroundColor Green
} else {
    Write-Host "❌ Error en instalación" -ForegroundColor Red
    exit 1
}

# 3. Verificar permisos específicos para Honor
Write-Host "🔐 Configurando permisos específicos para Honor..." -ForegroundColor Yellow

# Permisos críticos para Honor X6b Plus
$permissions = @(
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA", 
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.POST_NOTIFICATIONS"
)

foreach ($permission in $permissions) {
    Write-Host "  📋 Otorgando permiso: $permission" -ForegroundColor Cyan
    adb shell pm grant com.example.demoappchat $permission
}

# 4. Configurar optimizaciones específicas para Honor
Write-Host "⚡ Aplicando optimizaciones Honor X6b Plus..." -ForegroundColor Yellow

# Desactivar optimización de batería
adb shell dumpsys deviceidle whitelist +com.example.demoappchat

# Configurar inicio automático (si es posible)
adb shell am start -n "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity"

# 5. Iniciar la aplicación
Write-Host "🎯 Iniciando aplicación..." -ForegroundColor Yellow
adb shell am start -n "com.example.demoappchat/.MainActivity"

# 6. Monitorear logs específicos para Honor
Write-Host "📊 Monitoreando logs (Honor X6b Plus)..." -ForegroundColor Yellow
Write-Host "   Presiona Ctrl+C para detener el monitoreo" -ForegroundColor Gray

# Monitorear logs específicos para problemas de Honor
adb logcat -s "VoiceService" -s "MainActivity" -s "BackgroundVoiceService" | ForEach-Object {
    if ($_ -match "Honor|HONOR|Huawei|HUAWEI") {
        Write-Host $_ -ForegroundColor Yellow
    } elseif ($_ -match "Error|ERROR|❌") {
        Write-Host $_ -ForegroundColor Red  
    } elseif ($_ -match "✅|SUCCESS|Successful") {
        Write-Host $_ -ForegroundColor Green
    } else {
        Write-Host $_
    }
}