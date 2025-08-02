package com.example.demoappchat.domain.usecase.voice

import com.example.demoappchat.domain.repository.VoiceRepository
import com.example.demoappchat.domain.repository.VoiceServiceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case para monitorear el servicio de voz
 */
class MonitorVoiceServiceUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    
    fun execute(): Flow<VoiceServiceStatus> {
        return voiceRepository.getVoiceServiceStatus()
    }
}