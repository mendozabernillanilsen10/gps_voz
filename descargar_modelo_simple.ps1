# Descarga simple del modelo Whisper
Write-Host "🚀 Descargando modelo Whisper Base (142MB)..." -ForegroundColor Green
Write-Host ""

$modelDir = "app/src/main/assets"
$modelFile = "ggml-base.bin"
$outputPath = Join-Path $modelDir $modelFile
$url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"

# Crear directorio si no existe
if (!(Test-Path $modelDir)) {
    New-Item -ItemType Directory -Path $modelDir -Force | Out-Null
}

# Verificar si ya existe
if (Test-Path $outputPath) {
    $fileSize = (Get-Item $outputPath).Length / 1MB
    Write-Host "✅ Modelo ya existe: $([math]::Round($fileSize, 2))MB" -ForegroundColor Green
    Write-Host "📁 $outputPath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Compila con: ./gradlew clean assembleDebug" -ForegroundColor Yellow
    exit 0
}

# Descargar
Write-Host "📦 Descargando desde HuggingFace..." -ForegroundColor Cyan
Write-Host "⏳ Esto puede tomar 2-5 minutos..." -ForegroundColor Yellow
Write-Host ""

try {
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $outputPath
    $ProgressPreference = 'Continue'
    
    $fileSize = (Get-Item $outputPath).Length / 1MB
    Write-Host ""
    Write-Host "✅ ¡Descarga completada!" -ForegroundColor Green
    Write-Host "📊 Tamaño: $([math]::Round($fileSize, 2))MB" -ForegroundColor Cyan
    Write-Host "📁 $outputPath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🎯 Siguiente paso: ./gradlew clean assembleDebug" -ForegroundColor Yellow
    
} catch {
    Write-Host ""
    Write-Host "❌ Error descargando: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Descarga manual:" -ForegroundColor Yellow
    Write-Host "   URL: $url" -ForegroundColor Gray
    Write-Host "   Guardar en: $outputPath" -ForegroundColor Gray
    exit 1
}





