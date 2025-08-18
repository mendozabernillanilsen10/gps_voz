package com.example.demoappchat.di

import com.example.demoappchat.data.service.ErrorLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para servicios
 * Configura servicios como ErrorLogger
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideErrorLogger(): ErrorLogger {
        return ErrorLogger()
    }
}