package com.example.scamguardv1.domain.usecase

import com.example.scamguardv1.data.AudioFileRepository
import com.example.scamguardv1.domain.model.AudioFile

class GetAudioFilesUseCase(private val repository: AudioFileRepository) {
    suspend operator fun invoke(): Result<List<AudioFile>> {
        return try {
            val files = repository.queryAudioFiles()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
