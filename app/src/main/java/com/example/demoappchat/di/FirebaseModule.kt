package com.example.demoappchat.di

import android.content.Context
import com.example.demoappchat.data.service.WebRTCSignalingService
import com.example.demoappchat.data.webrtc.WebRTCClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideWebRTCSignalingService(database: FirebaseDatabase): WebRTCSignalingService {
        return WebRTCSignalingService(database)
    }
    
    @Provides
    @Singleton
    fun provideWebRTCClient(
        @ApplicationContext context: Context,
        signalingService: WebRTCSignalingService
    ): WebRTCClient {
        return WebRTCClient(context, signalingService)
    }
} 