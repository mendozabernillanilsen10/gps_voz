package com.example.demoappchat.di

import com.example.demoappchat.data.repository.ChatRepositoryImpl
import com.example.demoappchat.data.repository.LocationRepositoryImpl
import com.example.demoappchat.data.repository.MediaRepositoryImpl
import com.example.demoappchat.data.repository.PermissionRepositoryImpl
import com.example.demoappchat.data.repository.VoiceCommandsRepository
import com.example.demoappchat.domain.repository.ChatRepository
import com.example.demoappchat.domain.repository.LocationRepository
import com.example.demoappchat.domain.repository.MediaRepository
import com.example.demoappchat.domain.repository.PermissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para repositorios
 * Configura todas las implementaciones de repositorios de dominio
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        permissionRepositoryImpl: PermissionRepositoryImpl
    ): PermissionRepository
}