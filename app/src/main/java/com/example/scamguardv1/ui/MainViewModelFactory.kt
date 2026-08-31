package com.example.scamguardv1.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scamguardv1.domain.player.VoicePlayer
import com.example.scamguardv1.domain.usecase.AnalyzeVoiceUseCase
import com.example.scamguardv1.domain.usecase.GetAudioFilesUseCase

class MainViewModelFactory(
    private val context: Context,
    private val getAudioFilesUseCase: GetAudioFilesUseCase,
    private val analyzeVoiceUseCase: AnalyzeVoiceUseCase,
    private val voicePlayer: VoicePlayer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context.applicationContext, getAudioFilesUseCase, analyzeVoiceUseCase, voicePlayer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
