# Script para descargar modelo Whisper para Android
# Ejecutar desde la raíz del proyecto

Write-Host "🚀 Descargando modelo Whisper..." -ForegroundColor Green
Write-Host ""

$modelDir = "app/src/main/assets"
$modelsAvailable = @(
    @{
        Name = "ggml-tiny.bin"
        Size = "75MB"
        Url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
        Description = "Modelo Tiny - Más rápido, menor precisión"
    },
    @{
        Name = "ggml-base.bin"
        Size = "142MB"
        Url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        Description = "Modelo Base - Balance perfecto (RECOMENDADO)"
    },
    @{
        Name = "ggml-small.bin"
        Size = "466MB"
        Url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
        Description = "Modelo Small - Más preciso, más lento"
    }
)

Write-Host "Modelos disponibles:" -ForegroundColor Cyan
Write-Host ""
for ($i = 0; $i -lt $modelsAvailable.Count; $i++) {
    $model = $modelsAvailable[$i]
    Write-Host "  [$($i+1)] $($model.Name) - $($model.Size)"
    Write-Host "      $($model.Description)" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Selecciona modelo (1-3) [2 recomendado]: " -NoNewline -ForegroundColor Yellow
$selection = Read-Host
if ([string]::IsNullOrWhiteSpace($selection)) {
    $selection = "2"
}

$selectedIndex = [int]$selection - 1
if ($selectedIndex -lt 0 -or $selectedIndex -ge $modelsAvailable.Count) {
    Write-Host "❌ Selección inválida" -ForegroundColor Red
    exit 1
}

$selectedModel = $modelsAvailable[$selectedIndex]

Write-Host ""
Write-Host "📦 Descargando: $($selectedModel.Name) ($($selectedModel.Size))..." -ForegroundColor Green
Write-Host ""

# Crear directorio si no existe
if (!(Test-Path $modelDir)) {
    New-Item -ItemType Directory -Path $modelDir -Force | Out-Null
}

$outputPath = Join-Path $modelDir $selectedModel.Name

# Verificar si ya existe
if (Test-Path $outputPath) {
    Write-Host "⚠️  El archivo ya existe: $outputPath" -ForegroundColor Yellow
    Write-Host "¿Descargar de nuevo? (s/N): " -NoNewline
    $overwrite = Read-Host
    if ($overwrite -ne "s" -and $overwrite -ne "S") {
        Write-Host "✅ Usando modelo existente" -ForegroundColor Green
        exit 0
    }
    Remove-Item $outputPath -Force
}

# Descargar con progreso
try {
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $selectedModel.Url -OutFile $outputPath
    $ProgressPreference = 'Continue'
    
    Write-Host ""
    Write-Host "✅ Modelo descargado exitosamente!" -ForegroundColor Green
    Write-Host "📁 Ubicación: $outputPath" -ForegroundColor Cyan
    Write-Host ""
    
    $fileSize = (Get-Item $outputPath).Length / 1MB
    Write-Host "📊 Tamaño: $([math]::Round($fileSize, 2))MB" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🎯 Ahora puedes compilar la app con: ./gradlew assembleDebug" -ForegroundColor Green
    
} catch {
    Write-Host ""
    Write-Host "❌ Error descargando modelo: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Descarga manual:" -ForegroundColor Yellow
    Write-Host "   1. Visita: $($selectedModel.Url)" -ForegroundColor Gray
    Write-Host "   2. Guarda como: $outputPath" -ForegroundColor Gray
    exit 1
}

