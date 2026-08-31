package com.example.scamguardv1.domain.usecase

import com.example.scamguardv1.data.ScamDetector
import com.example.scamguardv1.data.VoiceAnalyzer
import com.example.scamguardv1.domain.model.AudioFile
import com.example.scamguardv1.domain.model.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyzeVoiceUseCase(
    private val voiceAnalyzer: VoiceAnalyzer,
    private val scamDetector: ScamDetector
) {
    suspend operator fun invoke(file: AudioFile): Result<DetectionResult> = withContext(Dispatchers.IO) {
        try {
            val audioData = voiceAnalyzer.getAudioData(file.uri)
            if (audioData != null) {
                val features = voiceAnalyzer.extractFeatures(audioData)
                val result = scamDetector.analyzeVoice(features)
                Result.success(result)
            } else {
                Result.failure(Exception("Could not read audio data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
