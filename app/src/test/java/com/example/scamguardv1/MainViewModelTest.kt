package com.example.scamguardv1

import android.content.Context
import com.example.scamguardv1.data.AudioFileRepository
import com.example.scamguardv1.data.HistoryManager
import com.example.scamguardv1.data.ScamDetector
import com.example.scamguardv1.data.VoiceAnalyzer
import com.example.scamguardv1.domain.player.VoicePlayer
import com.example.scamguardv1.domain.usecase.AnalyzeVoiceUseCase
import com.example.scamguardv1.domain.usecase.GetAudioFilesUseCase
import com.example.scamguardv1.ui.MainViewModel
import com.example.scamguardv1.ui.state.MainUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    lateinit var getAudioFilesUseCase: GetAudioFilesUseCase

    @Mock
    lateinit var analyzeVoiceUseCase: AnalyzeVoiceUseCase

    @Mock
    lateinit var voicePlayer: VoicePlayer

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var historyManager: HistoryManager

    @Mock
    lateinit var voiceAnalyzer: VoiceAnalyzer

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(context, getAudioFilesUseCase, analyzeVoiceUseCase, voicePlayer, historyManager, voiceAnalyzer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsWelcome() = runTest {
        assertEquals(MainUiState.Welcome, viewModel.uiState.value)
    }

    @Test
    fun testGetStartedTransitionsToCheckingPermission() = runTest {
        viewModel.getStarted()
        assertEquals(MainUiState.CheckingPermission, viewModel.uiState.value)
    }

    @Test
    fun testPermissionGrantedTransitionsToLoadingThenSuccess() = runTest {
        `when`(getAudioFilesUseCase()).thenReturn(Result.success(emptyList()))
        
        viewModel.onPermissionResult(true)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value is MainUiState.Success)
    }

    @Test
    fun testPermissionDeniedTransitionsToPermissionDeniedState() = runTest {
        viewModel.onPermissionResult(false)
        assertEquals(MainUiState.PermissionDenied, viewModel.uiState.value)
    }
}
