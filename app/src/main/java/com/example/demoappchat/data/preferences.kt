package com.example.demoappchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // User Settings
        private val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        
        // Voice Settings
        private val VOICE_SERVICE_ENABLED = booleanPreferencesKey("voice_service_enabled")
        private val DISCRETE_MODE = booleanPreferencesKey("discrete_mode")
        private val VOICE_COMMANDS = stringSetPreferencesKey("voice_commands")
        private val CURRENT_CHAT_ID = stringPreferencesKey("current_chat_id")
        
        // Audio Settings
        private val TRANSMISSION_RADIUS = floatPreferencesKey("transmission_radius")
        private val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val AUTO_UPLOAD_RECORDINGS = booleanPreferencesKey("auto_upload_recordings")
        
        // Privacy Settings
        private val SHARE_LOCATION = booleanPreferencesKey("share_location")
        private val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
    }

    private val dataStore = context.dataStore

    // User Settings
    suspend fun setFirstTime(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME] = isFirstTime
        }
    }

    fun getFirstTime(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_FIRST_TIME] ?: true
        }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID]
        }
    }

    suspend fun setUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = userName
        }
    }

    fun getUserName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }
    }

    suspend fun setUserEmail(userEmail: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = userEmail
        }
    }

    fun getUserEmail(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL]
        }
    }

    // Voice Settings
    suspend fun setVoiceServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_SERVICE_ENABLED] = enabled
        }
    }

    fun getVoiceServiceEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[VOICE_SERVICE_ENABLED] ?: false
        }
    }

    suspend fun setDiscreteMode(discrete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DISCRETE_MODE] = discrete
        }
    }

    fun getDiscreteMode(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DISCRETE_MODE] ?: true
        }
    }

    suspend fun setVoiceCommands(commands: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_COMMANDS] = commands.toSet()
        }
    }

    fun getVoiceCommands(): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[VOICE_COMMANDS]?.toList() ?: listOf("óyeme", "alerta", "grabar video", "ayuda")
        }
    }

    suspend fun setCurrentChatId(chatId: String?) {
        context.dataStore.edit { preferences ->
            if (chatId != null) {
                preferences[CURRENT_CHAT_ID] = chatId
            } else {
                preferences.remove(CURRENT_CHAT_ID)
            }
        }
    }

    fun getCurrentChatId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[CURRENT_CHAT_ID]
        }
    }

    // Audio Settings
    suspend fun setTransmissionRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[TRANSMISSION_RADIUS] = radius
        }
    }

    fun getTransmissionRadius(): Flow<Float> {
        return context.dataStore.data.map { preferences ->
            preferences[TRANSMISSION_RADIUS] ?: 4.0f
        }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY] = quality
        }
    }

    fun getAudioQuality(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[AUDIO_QUALITY] ?: "Media"
        }
    }

    suspend fun setAutoUploadRecordings(autoUpload: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPLOAD_RECORDINGS] = autoUpload
        }
    }

    fun getAutoUploadRecordings(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_UPLOAD_RECORDINGS] ?: true
        }
    }

    // Privacy Settings
    suspend fun setShareLocation(share: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHARE_LOCATION] = share
        }
    }

    fun getShareLocation(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SHARE_LOCATION] ?: true
        }
    }

    suspend fun setDataRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[DATA_RETENTION_DAYS] = days
        }
    }

    fun getDataRetentionDays(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DATA_RETENTION_DAYS] ?: 7
        }
    }
}