# Script para aplicar las reglas de Firebase corregidas
# Soluciona el error "Permission denied" en chat_participants

Write-Host "🔥 Aplicando reglas de Firebase corregidas..." -ForegroundColor Green

# Verificar si Firebase CLI está instalado
try {
    firebase --version | Out-Null
    Write-Host "✅ Firebase CLI detectado" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI no encontrado. Instálalo con: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar si el usuario está autenticado
try {
    firebase projects:list | Out-Null
    Write-Host "✅ Usuario autenticado en Firebase" -ForegroundColor Green
} catch {
    Write-Host "❌ No autenticado en Firebase. Ejecuta: firebase login" -ForegroundColor Red
    exit 1
}

# Aplicar las reglas de Firebase
Write-Host "📋 Aplicando reglas corregidas desde firebase_rules.json..." -ForegroundColor Yellow

try {
    firebase deploy --only database
    Write-Host "✅ Reglas de Firebase aplicadas exitosamente!" -ForegroundColor Green
    Write-Host "🎯 Correcciones aplicadas:" -ForegroundColor Cyan
    Write-Host "   - chat_participants: Permite valores booleanos y objetos completos" -ForegroundColor White
    Write-Host "   - Validación flexible: newData.hasChildren(['joinedAt', 'isActive']) || newData.isBoolean()" -ForegroundColor White
    Write-Host "   - Compatibilidad con código existente y nuevo" -ForegroundColor White
} catch {
    Write-Host "❌ Error aplicando reglas de Firebase: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎉 ¡Configuración completada!" -ForegroundColor Green
Write-Host "📱 El error 'Permission denied' en chat_participants debería estar resuelto" -ForegroundColor Cyan
Write-Host "🔧 Los usuarios ahora pueden unirse a chats grupales sin problemas" -ForegroundColor Cyan
