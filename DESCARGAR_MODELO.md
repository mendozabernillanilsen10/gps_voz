# 📦 Descargar Modelo Whisper

## Opción 1: Script Automático (Recomendado)

Abre **PowerShell** en la raíz del proyecto y ejecuta:

```powershell
.\download_whisper_model.ps1
```

Selecciona `[2]` para el modelo **Base** (142MB) - **RECOMENDADO**

---

## Opción 2: Descarga Manual

1. **Descarga el modelo Base:**
   - URL: https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
   - Tamaño: 142MB

2. **Guárdalo en:**
   ```
   app/src/main/assets/ggml-base.bin
   ```

3. **Crear carpeta si no existe:**
   ```powershell
   mkdir app\src\main\assets
   ```

4. **Mover archivo:**
   ```powershell
   move ggml-base.bin app\src\main\assets\
   ```

---

## Verificar

Confirma que el archivo existe:

```powershell
dir app\src\main\assets\ggml-base.bin
```

Deberías ver: `ggml-base.bin` (aproximadamente 142 MB)

---

## Siguiente Paso

Una vez descargado, ejecuta:

```bash
./gradlew assembleDebug
```

¡Y listo! Tu app ahora usa Whisper 🎉

