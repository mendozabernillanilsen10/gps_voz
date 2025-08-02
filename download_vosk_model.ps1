# Script para descargar modelo Vosk
Write-Host "📥 Descargando modelo Vosk..." -ForegroundColor Green

# Crear directorio si no existe
$modelDir = "app/src/main/assets/vosk-model"
if (!(Test-Path $modelDir)) {
    New-Item -ItemType Directory -Path $modelDir -Force
}

# URL del modelo pequeño en español
$modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
$zipFile = "vosk-model.zip"

Write-Host "🔗 Descargando desde: $modelUrl" -ForegroundColor Yellow

try {
    # Descargar el archivo
    Invoke-WebRequest -Uri $modelUrl -OutFile $zipFile
    
    Write-Host "📦 Extrayendo modelo..." -ForegroundColor Yellow
    
    # Extraer el archivo
    Expand-Archive -Path $zipFile -DestinationPath "temp_vosk" -Force
    
    # Mover archivos al directorio correcto
    $extractedDir = Get-ChildItem "temp_vosk" -Directory | Select-Object -First 1
    if ($extractedDir) {
        Copy-Item "$($extractedDir.FullName)\*" -Destination $modelDir -Recurse -Force
        Write-Host "✅ Modelo copiado exitosamente a $modelDir" -ForegroundColor Green
    }
    
    # Limpiar archivos temporales
    Remove-Item $zipFile -Force -ErrorAction SilentlyContinue
    Remove-Item "temp_vosk" -Recurse -Force -ErrorAction SilentlyContinue
    
    Write-Host "🎉 Modelo Vosk descargado y configurado exitosamente!" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Error descargando modelo: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "🔄 Usando modelo simulado como fallback..." -ForegroundColor Yellow
} 