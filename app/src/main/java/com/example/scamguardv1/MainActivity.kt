package com.example.scamguardv1

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.activity.viewModels
import com.example.scamguardv1.data.AudioFileRepository
import com.example.scamguardv1.data.ExoVoicePlayer
import com.example.scamguardv1.data.ScamDetector
import com.example.scamguardv1.data.VoiceAnalyzer
import com.example.scamguardv1.domain.usecase.AnalyzeVoiceUseCase
import com.example.scamguardv1.domain.usecase.GetAudioFilesUseCase
import com.example.scamguardv1.ui.MainViewModel
import com.example.scamguardv1.ui.MainViewModelFactory
import com.example.scamguardv1.ui.screens.ScamGuardApp
import com.example.scamguardv1.ui.theme.ScamGuardV1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        val voiceAnalyzer = VoiceAnalyzer(applicationContext)
        val scamDetector = ScamDetector(applicationContext)
        val repository = AudioFileRepository(applicationContext)
        val voicePlayer = ExoVoicePlayer(applicationContext)
        
        val getAudioFilesUseCase = GetAudioFilesUseCase(repository)
        val analyzeVoiceUseCase = AnalyzeVoiceUseCase(voiceAnalyzer, scamDetector)
        val historyManager = com.example.scamguardv1.data.HistoryManager(applicationContext)
        
        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(this, getAudioFilesUseCase, analyzeVoiceUseCase, voicePlayer, historyManager, voiceAnalyzer)
        }

        setContent {
            ScamGuardV1Theme {
                ScamGuardApp(viewModel)
            }
        }
    }
}
