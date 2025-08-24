package com.example.demoappchat.di

import com.example.demoappchat.domain.repository.ChatRepository
import com.example.demoappchat.domain.repository.LocationRepository
import com.example.demoappchat.domain.repository.MediaRepository
import com.example.demoappchat.domain.repository.PermissionRepository
import com.example.demoappchat.domain.repository.VoiceRepository
import com.example.demoappchat.domain.usecase.voice.ExecuteVoiceCommandUseCase
import com.example.demoappchat.domain.usecase.voice.MonitorVoiceServiceUseCase
import com.example.demoappchat.domain.usecase.voice.ProcessVoiceRecognitionUseCase
import com.example.demoappchat.domain.usecase.voice.StartVoiceRecognitionUseCase
import com.example.demoappchat.domain.usecase.voice.StopVoiceRecognitionUseCase
import com.example.demoappchat.domain.usecase.voice.StartBackgroundVoiceServiceUseCase
import com.example.demoappchat.domain.usecase.voice.StopBackgroundVoiceServiceUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context

/**
 * Módulo de inyección de dependencias para Use Cases
 * Configura todos los casos de uso del dominio
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // TODO: Implementar cuando las dependencias estén disponibles
    // @Provides
    // @Singleton
    // fun provideStartVoiceRecognitionUseCase(
    //     voiceRepository: VoiceRepository,
    //     permissionRepository: PermissionRepository
    // ): StartVoiceRecognitionUseCase = StartVoiceRecognitionUseCase(
    //     voiceRepository, permissionRepository
    // )

    // TODO: Implementar cuando las dependencias estén disponibles
    // @Provides
    // @Singleton
    // fun provideStopVoiceRecognitionUseCase(
    //     voiceRepository: VoiceRepository
    // ): StopVoiceRecognitionUseCase = StopVoiceRecognitionUseCase(voiceRepository)
    //
    // @Provides
    // @Singleton
    // fun provideExecuteVoiceCommandUseCase(
    //     chatRepository: ChatRepository,
    //     mediaRepository: MediaRepository,
    //     locationRepository: LocationRepository,
    //     voiceRepository: VoiceRepository
    // ): ExecuteVoiceCommandUseCase = ExecuteVoiceCommandUseCase(
    //     chatRepository, mediaRepository, locationRepository, voiceRepository
    // )
    //
    // @Provides
    // @Singleton
    // fun provideProcessVoiceRecognitionUseCase(
    //     voiceRepository: VoiceRepository,
    //     executeVoiceCommandUseCase: ExecuteVoiceCommandUseCase
    // ): ProcessVoiceRecognitionUseCase = ProcessVoiceRecognitionUseCase(
    //     voiceRepository, executeVoiceCommandUseCase
    // )
    //
    // @Provides
    // @Singleton
    // fun provideMonitorVoiceServiceUseCase(
    //     voiceRepository: VoiceRepository
    // ): MonitorVoiceServiceUseCase = MonitorVoiceServiceUseCase(voiceRepository)

    @Provides
    @Singleton
    fun provideStartBackgroundVoiceServiceUseCase(
        @ApplicationContext context: Context
    ): StartBackgroundVoiceServiceUseCase = StartBackgroundVoiceServiceUseCase(context)

    @Provides
    @Singleton
    fun provideStopBackgroundVoiceServiceUseCase(
        @ApplicationContext context: Context
    ): StopBackgroundVoiceServiceUseCase = StopBackgroundVoiceServiceUseCase(context)
}