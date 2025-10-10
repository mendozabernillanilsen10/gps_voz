$url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
$output = "app/src/main/assets/ggml-base.bin"
$dir = "app/src/main/assets"

if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

if (Test-Path $output) {
    Write-Host "Modelo ya existe"
    exit 0
}

Write-Host "Descargando modelo (142MB)..."
Invoke-WebRequest -Uri $url -OutFile $output
Write-Host "Descarga completa!"

