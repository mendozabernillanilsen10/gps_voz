// data/VoiceServicePreferences.kt
package com.example.demoappchat.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceServicePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "safevoice_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_VOICE_SERVICE_ENABLED = "voice_service_enabled"
        private const val KEY_ACTIVATION_COMMANDS = "activation_commands"
        private const val KEY_EMERGENCY_RADIUS = "emergency_radius"
        private const val KEY_AUTO_SEND_LOCATION = "auto_send_location"
        private const val KEY_DISCRETE_MODE = "discrete_mode"
        private const val KEY_CURRENT_CHAT_ID = "current_chat_id"
    }

    var isVoiceServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_SERVICE_ENABLED, value).apply()

    var activationCommands: Set<String>
        get() = prefs.getStringSet(KEY_ACTIVATION_COMMANDS, setOf("óyeme", "saa", "alerta", "ayuda")) ?: setOf()
        set(value) = prefs.edit().putStringSet(KEY_ACTIVATION_COMMANDS, value).apply()

    var emergencyRadius: Int
        get() = prefs.getInt(KEY_EMERGENCY_RADIUS, 300)
        set(value) = prefs.edit().putInt(KEY_EMERGENCY_RADIUS, value).apply()

    var autoSendLocation: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SEND_LOCATION, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SEND_LOCATION, value).apply()

    var discreteMode: Boolean
        get() = prefs.getBoolean(KEY_DISCRETE_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DISCRETE_MODE, value).apply()

    var currentChatId: String?
        get() = prefs.getString(KEY_CURRENT_CHAT_ID, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_CHAT_ID, value).apply()
}