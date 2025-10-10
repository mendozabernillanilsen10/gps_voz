package com.example.demoappchat.data.service.voice

import android.util.Log

/**
 * Agente inteligente para interpretar comandos de voz
 * Usa múltiples estrategias para matching flexible
 */
class CommandAgent {
    
    private val TAG = "CommandAgent"
    
    /**
     * Interpretar texto y encontrar comando coincidente
     */
    fun interpret(text: String, commands: List<VoiceCommand>): CommandMatch? {
        if (text.isBlank() || commands.isEmpty()) {
            return null
        }
        
        val normalizedText = normalizeText(text)
        Log.d(TAG, "🔍 Interpretando: '$text' → '$normalizedText'")
        Log.d(TAG, "📋 Comandos disponibles: ${commands.map { it.keyword }}")
        
        // Estrategia 1: Match exacto por keyword
        var match = findExactMatch(normalizedText, commands)
        if (match != null) {
            Log.d(TAG, "✅ MATCH EXACTO: '${match.command.keyword}'")
            return match
        }
        
        // Estrategia 2: Match por sinónimos
        match = findSynonymMatch(normalizedText, commands)
        if (match != null) {
            Log.d(TAG, "✅ MATCH SINÓNIMO: '${match.command.keyword}' vía '${match.matchedBy}'")
            return match
        }
        
        // Estrategia 3: Match por prefijo ("al" → "alerta")
        match = findPrefixMatch(normalizedText, commands)
        if (match != null) {
            Log.d(TAG, "✅ MATCH PREFIJO: '${match.command.keyword}' (prefijo: '${match.matchedBy}')")
            return match
        }
        
        // Estrategia 4: Match por contención
        match = findContainsMatch(normalizedText, commands)
        if (match != null) {
            Log.d(TAG, "✅ MATCH CONTENCIÓN: '${match.command.keyword}'")
            return match
        }
        
        // Estrategia 5: Match por similitud (Levenshtein)
        match = findSimilarMatch(normalizedText, commands)
        if (match != null) {
            Log.d(TAG, "✅ MATCH SIMILAR: '${match.command.keyword}' (similitud: ${match.confidence})")
            return match
        }
        
        Log.d(TAG, "❌ No se encontró comando para: '$text'")
        return null
    }
    
    /**
     * Match exacto por keyword o alias
     */
    private fun findExactMatch(text: String, commands: List<VoiceCommand>): CommandMatch? {
        for (command in commands) {
            val normalizedKeyword = normalizeText(command.keyword)
            
            if (text == normalizedKeyword) {
                return CommandMatch(
                    command = command,
                    matchedBy = command.keyword,
                    matchType = MatchType.EXACT,
                    confidence = 1.0f
                )
            }
        }
        return null
    }
    
    /**
     * Match por sinónimos
     */
    private fun findSynonymMatch(text: String, commands: List<VoiceCommand>): CommandMatch? {
        for (command in commands) {
            for (synonym in command.synonyms) {
                val normalizedSynonym = normalizeText(synonym)
                
                if (text == normalizedSynonym || text.contains(normalizedSynonym)) {
                    return CommandMatch(
                        command = command,
                        matchedBy = synonym,
                        matchType = MatchType.SYNONYM,
                        confidence = 0.95f
                    )
                }
            }
        }
        return null
    }
    
    /**
     * Match por prefijo - muy importante para audio desde bolsillo
     * "al" coincide con "alerta"
     * "oy" coincide con "óyeme"
     * "grav" coincide con "grabar"
     */
    private fun findPrefixMatch(text: String, commands: List<VoiceCommand>): CommandMatch? {
        if (text.length < 2) return null
        
        for (command in commands) {
            val normalizedKeyword = normalizeText(command.keyword)
            
            // 1. Keyword empieza con el texto (muy flexible)
            if (normalizedKeyword.startsWith(text)) {
                return CommandMatch(
                    command = command,
                    matchedBy = text,
                    matchType = MatchType.PREFIX,
                    confidence = 0.90f
                )
            }
            
            // 2. Texto empieza con el keyword (parcial)
            if (text.startsWith(normalizedKeyword) && text.length <= normalizedKeyword.length + 3) {
                return CommandMatch(
                    command = command,
                    matchedBy = normalizedKeyword,
                    matchType = MatchType.PREFIX,
                    confidence = 0.85f
                )
            }
            
            // 3. Primeras 3 letras coinciden (muy importante para audio muffled)
            if (text.length >= 3 && normalizedKeyword.length >= 3) {
                val textPrefix = text.take(3)
                val keywordPrefix = normalizedKeyword.take(3)
                if (textPrefix == keywordPrefix) {
                    return CommandMatch(
                        command = command,
                        matchedBy = textPrefix,
                        matchType = MatchType.PREFIX,
                        confidence = 0.80f
                    )
                }
            }
            
            // 4. Primeras 2 letras coinciden (para palabras muy cortas)
            if (text.length >= 2 && normalizedKeyword.length >= 2) {
                val textPrefix = text.take(2)
                val keywordPrefix = normalizedKeyword.take(2)
                if (textPrefix == keywordPrefix) {
                    return CommandMatch(
                        command = command,
                        matchedBy = textPrefix,
                        matchType = MatchType.PREFIX,
                        confidence = 0.75f
                    )
                }
            }
        }
        return null
    }
    
    /**
     * Match por contención
     */
    private fun findContainsMatch(text: String, commands: List<VoiceCommand>): CommandMatch? {
        for (command in commands) {
            val normalizedKeyword = normalizeText(command.keyword)
            
            if (text.contains(normalizedKeyword) || normalizedKeyword.contains(text)) {
                return CommandMatch(
                    command = command,
                    matchedBy = normalizedKeyword,
                    matchType = MatchType.CONTAINS,
                    confidence = 0.75f
                )
            }
        }
        return null
    }
    
    /**
     * Match por similitud usando distancia de Levenshtein
     * Ahora más permisivo: 50% en lugar de 60%
     */
    private fun findSimilarMatch(text: String, commands: List<VoiceCommand>): CommandMatch? {
        var bestMatch: CommandMatch? = null
        var bestSimilarity = 0.0f
        
        for (command in commands) {
            val normalizedKeyword = normalizeText(command.keyword)
            val distance = levenshteinDistance(text, normalizedKeyword)
            val maxLen = maxOf(text.length, normalizedKeyword.length)
            val similarity = 1.0f - (distance.toFloat() / maxLen)
            
            // Aceptar si similaridad > 50% (más permisivo)
            if (similarity >= 0.5f && similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = CommandMatch(
                    command = command,
                    matchedBy = normalizedKeyword,
                    matchType = MatchType.SIMILAR,
                    confidence = similarity
                )
            }
        }
        
        return bestMatch
    }
    
    /**
     * Normalizar texto: minúsculas, sin acentos, sin espacios extra
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            // Remover acentos
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }
    
    /**
     * Calcular distancia de Levenshtein
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // deletion
                    dp[i][j - 1] + 1,        // insertion
                    dp[i - 1][j - 1] + cost  // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
}

/**
 * Comando de voz configurado por el usuario
 */
data class VoiceCommand(
    val keyword: String,           // Palabra clave principal: "alerta", "emergencia"
    val action: String,            // Acción a ejecutar: "CREATE_ALERT_CHAT"
    val synonyms: List<String> = emptyList(),  // Sinónimos: "aviso", "advertencia"
    val description: String = ""   // Descripción para el usuario
)

/**
 * Resultado del matching
 */
data class CommandMatch(
    val command: VoiceCommand,
    val matchedBy: String,         // Qué palabra/prefijo hizo match
    val matchType: MatchType,
    val confidence: Float          // 0.0 - 1.0
)

/**
 * Tipos de matching
 */
enum class MatchType {
    EXACT,      // Match exacto
    SYNONYM,    // Match por sinónimo
    PREFIX,     // Match por prefijo
    CONTAINS,   // Match por contención
    SIMILAR     // Match por similitud
}

