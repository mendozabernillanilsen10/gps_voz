package com.example.demoappchat.di

import com.example.demoappchat.data.local.preferences.VoicePreferences
import com.example.demoappchat.data.repository.VoiceRepositoryImpl
import com.example.demoappchat.data.service.voice.VoiceEngineManager
import com.example.demoappchat.domain.repository.VoiceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO: Implementar cuando las dependencias estén disponibles
// @Module
// @InstallIn(SingletonComponent::class)
// object VoiceModule {
//     
//     @Provides
//     @Singleton
//     fun provideVoicePreferences(): VoicePreferences {
//         return VoicePreferences()
//     }
//     
//     @Provides
//     @Singleton
//     fun provideVoiceEngineManager(): VoiceEngineManager {
//         return VoiceEngineManager()
//     }
//     
//     @Provides
//     @Singleton
//     fun provideVoiceRepository(
//         voicePreferences: VoicePreferences,
//         voiceEngineManager: VoiceEngineManager
//     ): VoiceRepository {
//         return VoiceRepositoryImpl(voicePreferences, voiceEngineManager)
//     }
// }