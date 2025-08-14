# Script para aplicar las reglas de Firebase actualizadas
# Requiere Firebase CLI instalado

Write-Host "🔥 Aplicando reglas de Firebase actualizadas..." -ForegroundColor Green

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
Write-Host "📋 Aplicando reglas desde firebase_rules.json..." -ForegroundColor Yellow

try {
    firebase deploy --only database
    Write-Host "✅ Reglas de Firebase aplicadas exitosamente!" -ForegroundColor Green
    Write-Host "🎯 Los índices ahora están configurados para:" -ForegroundColor Cyan
    Write-Host "   - users: isActive, lastSeen, status" -ForegroundColor White
    Write-Host "   - proximity_chats: latitude, longitude, createdAt, isActive" -ForegroundColor White
    Write-Host "   - chat_messages: timestamp, userId, messageType" -ForegroundColor White
    Write-Host "   - chat_participants: joinedAt, isActive" -ForegroundColor White
    Write-Host "   - user_locations: latitude, longitude, lastUpdated" -ForegroundColor White
    Write-Host "   - webrtc_calls: chatId, callType, timestamp, status" -ForegroundColor White
    Write-Host "   - webrtc_signals: callId, timestamp" -ForegroundColor White
    Write-Host "   - chat_notifications: chatId, timestamp, type" -ForegroundColor White
    Write-Host "   - fcm_tokens: userId, deviceId" -ForegroundColor White
    Write-Host "   - notification_logs: timestamp, userId, type" -ForegroundColor White
    Write-Host "   - error_logs: timestamp, severity, userId" -ForegroundColor White
    Write-Host "   - group_calls: chatId, callType, timestamp, status" -ForegroundColor White
    Write-Host "   - voice_commands: userId, command, isActive" -ForegroundColor White
    Write-Host "   - voice_settings: userId, settingType" -ForegroundColor White
    Write-Host "   - media_recordings: chatId, userId, timestamp, mediaType" -ForegroundColor White
    Write-Host "   - emergency_alerts: userId, timestamp, status, priority" -ForegroundColor White
    Write-Host "   - surveillance_logs: userId, timestamp, actionType" -ForegroundColor White
} catch {
    Write-Host "❌ Error aplicando reglas de Firebase: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎉 ¡Configuración completada!" -ForegroundColor Green
Write-Host "📱 La aplicación ahora debería funcionar sin errores de índice" -ForegroundColor Cyan
