package com.example.scamguardv1.ui.state

import com.example.scamguardv1.domain.model.AudioFile
import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.domain.model.ScreenedCall

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class SettingsView {
    MAIN, THEME, AUTO_DELETE, ACCOUNT_SECURITY
}

sealed interface MainUiState {
    data object Splash : MainUiState
    data object Welcome : MainUiState
    data class Auth(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val infoMessage: String? = null,
        val isRegisterMode: Boolean = false,
        val userEmail: String? = null,
        val isEmailUnverified: Boolean = false
    ) : MainUiState
    data object CheckingPermission : MainUiState
    data object PermissionRequired : MainUiState
    data object PermissionDenied : MainUiState
    data object Loading : MainUiState
    data class VoiceRecordingRequired(
        val userEmail: String?,
        val isRecording: Boolean = false,
        val elapsedSeconds: Int = 0,
        val errorMessage: String? = null
    ) : MainUiState
    data class Success(
        val audioFiles: List<AudioFile>,
        val scanState: ScanUiState = ScanUiState.Idle,
        val history: Map<Long, DetectionResult> = emptyMap(),
        val screenedCalls: List<ScreenedCall> = emptyList(),
        val latestRiskScore: Int = 0,
        val isCallScreeningEnabled: Boolean = false,
        val isOverlayPermissionGranted: Boolean = false,
        val playingFileId: Long? = null,
        val isAudioPlaying: Boolean = false,
        val playbackPosition: Long = 0L,
        val playbackDuration: Long = 0L,
        val selectedAudioFile: AudioFile? = null,
        val isAudioScreeningViewVisible: Boolean = false,
        val isCallScreeningViewVisible: Boolean = false,
        val viewingResult: Pair<String, DetectionResult>? = null,
        val currentTheme: AppTheme = AppTheme.SYSTEM,
        val autoDeleteDays: Int = 0, // 0 means Never
        val isSettingsVisible: Boolean = false,
        val currentSettingsView: SettingsView = SettingsView.MAIN,
        val userEmail: String? = null,
        val userDisplayName: String? = null,
        val profilePictureUri: String? = null,
        val isEmailVerified: Boolean = false,
        val accountMessage: String? = null,
        val isAccountDeleting: Boolean = false
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Scanning(val file: AudioFile) : ScanUiState
    data class Success(val file: AudioFile, val result: DetectionResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
