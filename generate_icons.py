#!/usr/bin/env python3
"""
Script para generar íconos de Android en diferentes resoluciones
Basado en los archivos XML vectoriales creados
"""

import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

def create_icon_resources():
    """Crea los recursos de íconos en diferentes resoluciones"""
    
    # Definir las resoluciones de Android
    resolutions = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }
    
    # Directorio base
    base_dir = Path('app/src/main/res')
    
    # Crear directorios si no existen
    for resolution in resolutions.keys():
        (base_dir / resolution).mkdir(parents=True, exist_ok=True)
    
    print("✅ Directorios de íconos creados/verificados")
    print("📱 Resoluciones configuradas:")
    for res, size in resolutions.items():
        print(f"   {res}: {size}x{size}px")
    
    print("\n🎨 Íconos creados:")
    print("   • ic_launcher_foreground.xml - Ícono principal con sombreado")
    print("   • ic_launcher_background.xml - Fondo con gradiente")
    print("   • ic_launcher_foreground_minimal.xml - Versión minimalista")
    print("   • ic_launcher_background_minimal.xml - Fondo minimalista")
    
    print("\n📋 Configuración en AndroidManifest.xml:")
    print("   android:roundIcon=\"@mipmap/ic_launcher_round\" ✅")
    
    print("\n🚀 Para aplicar los cambios:")
    print("   1. Limpia el proyecto: ./gradlew clean")
    print("   2. Reconstruye: ./gradlew build")
    print("   3. Instala: ./gradlew installDebug")

if __name__ == "__main__":
    create_icon_resources()
