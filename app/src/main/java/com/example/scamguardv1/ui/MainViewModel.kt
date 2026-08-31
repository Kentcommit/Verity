package com.example.scamguardv1.ui

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scamguardv1.domain.model.AudioFile
import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.domain.model.ScreenedCall
import com.example.scamguardv1.domain.player.VoicePlayer
import com.example.scamguardv1.domain.usecase.AnalyzeVoiceUseCase
import com.example.scamguardv1.domain.usecase.GetAudioFilesUseCase
import com.example.scamguardv1.service.ScamCallScreeningService
import com.example.scamguardv1.ui.state.AppTheme
import com.example.scamguardv1.ui.state.MainUiState
import com.example.scamguardv1.ui.state.ScanUiState
import com.example.scamguardv1.utils.Constants
import com.example.scamguardv1.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(
    private val context: Context,
    private val getAudioFilesUseCase: GetAudioFilesUseCase,
    private val analyzeVoiceUseCase: AnalyzeVoiceUseCase,
    private val voicePlayer: VoicePlayer
) : ViewModel() {

    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Splash)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(2000)
            checkInitialState()
        }
    }

    private fun checkInitialState() {
        val permission = com.example.scamguardv1.utils.PermissionHelper.audioPermission
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            refreshFiles()
        } else {
            _uiState.value = MainUiState.Welcome
        }
    }

    private fun isPersistedCallScreeningEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_CALL_SCREENING_ENABLED, false)
    }

    private fun getPersistedLatestRiskScore(): Int {
        return prefs.getInt(Constants.PREF_LATEST_RISK_SCORE, 0)
    }

    private fun getPersistedTheme(): AppTheme {
        val themeName = prefs.getString(Constants.PREF_APP_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(themeName ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun getPersistedAutoDeleteDays(): Int {
        return prefs.getInt(Constants.PREF_AUTO_DELETE_DAYS, 0)
    }

    private fun persistTheme(theme: AppTheme) {
        prefs.edit().putString(Constants.PREF_APP_THEME, theme.name).apply()
    }

    private fun persistAutoDeleteDays(days: Int) {
        prefs.edit().putInt(Constants.PREF_AUTO_DELETE_DAYS, days).apply()
    }

    private fun persistLatestRiskScore(score: Int) {
        prefs.edit().putInt(Constants.PREF_LATEST_RISK_SCORE, score).apply()
    }

    private fun persistCallScreeningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_CALL_SCREENING_ENABLED, enabled).apply()
    }

    private fun persistScreenedCalls(calls: List<ScreenedCall>) {
        val array = JSONArray()
        calls.forEach { call ->
            val obj = JSONObject().apply {
                put("phoneNumber", call.phoneNumber)
                put("score", call.score.toDouble())
                put("reasons", JSONArray(call.reasons))
                put("timestamp", call.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(Constants.PREF_SCREENED_CALLS, array.toString()).apply()
    }

    private fun getPersistedScreenedCalls(): List<ScreenedCall> {
        val json = prefs.getString(Constants.PREF_SCREENED_CALLS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<ScreenedCall>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val reasonsArray = obj.getJSONArray("reasons")
                val reasons = List(reasonsArray.length()) { reasonsArray.getString(it) }
                list.add(
                    ScreenedCall(
                        phoneNumber = obj.getString("phoneNumber"),
                        score = obj.getDouble("score").toFloat(),
                        reasons = reasons,
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Logger.e("Failed to parse persisted calls", e)
            emptyList()
        }
    }

    private fun persistScanHistory(history: Map<Long, DetectionResult>) {
        val obj = JSONObject()
        history.forEach { (id, result) ->
            val resultObj = JSONObject().apply {
                put("score", result.score.toDouble())
                put("reasons", JSONArray(result.reasons))
            }
            obj.put(id.toString(), resultObj)
        }
        prefs.edit().putString(Constants.PREF_SCAN_HISTORY, obj.toString()).apply()
    }

    private fun getPersistedScanHistory(): Map<Long, DetectionResult> {
        val json = prefs.getString(Constants.PREF_SCAN_HISTORY, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<Long, DetectionResult>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val resultObj = obj.getJSONObject(id)
                val reasonsArray = resultObj.getJSONArray("reasons")
                val reasons = List(reasonsArray.length()) { reasonsArray.getString(it) }
                map[id.toLong()] = DetectionResult(
                    score = resultObj.getDouble("score").toFloat(),
                    reasons = reasons
                )
            }
            map
        } catch (e: Exception) {
            Logger.e("Failed to parse persisted history", e)
            emptyMap()
        }
    }

    private fun getDeletedAudioFileIds(): Set<Long> {
        val json = prefs.getString(Constants.PREF_DELETED_AUDIO_FILES, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val set = mutableSetOf<Long>()
            for (i in 0 until array.length()) {
                set.add(array.getLong(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun persistDeletedAudioFileIds(ids: Set<Long>) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        prefs.edit().putString(Constants.PREF_DELETED_AUDIO_FILES, array.toString()).apply()
    }

    private val callScreeningReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScamCallScreeningService.ACTION_CALL_SCREENED) {
                val phoneNumber = intent.getStringExtra(ScamCallScreeningService.EXTRA_PHONE_NUMBER) ?: "Unknown"
                val score = intent.getFloatExtra(ScamCallScreeningService.EXTRA_SCORE, 0f)
                val reasons = intent.getStringArrayExtra(ScamCallScreeningService.EXTRA_REASONS)?.toList() ?: emptyList()
                
                onCallScreened(ScreenedCall(phoneNumber, score, reasons))
            }
        }
    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter(ScamCallScreeningService.ACTION_CALL_SCREENED)
        ContextCompat.registerReceiver(
            context,
            callScreeningReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterReceiver(context: Context) {
        try {
            context.unregisterReceiver(callScreeningReceiver)
        } catch (e: Exception) {
            Logger.e("Failed to unregister receiver", e)
        }
    }

    private fun onCallScreened(call: ScreenedCall) {
        val scoreInt = call.score.toInt()
        persistLatestRiskScore(scoreInt)
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                val newList = listOf(call) + currentState.screenedCalls
                persistScreenedCalls(newList)
                currentState.copy(
                    screenedCalls = newList,
                    latestRiskScore = scoreInt
                )
            } else currentState
        }
    }

    fun toggleCallScreening(context: Context) {
        val currentState = _uiState.value as? MainUiState.Success ?: return

        if (!currentState.isCallScreeningEnabled) {
            checkAndRequestCallScreeningRole(context)
        } else {
            updateCallScreeningEnabled(false)
        }
    }

    fun checkOverlayPermission(context: Context) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(isOverlayPermissionGranted = granted)
            } else currentState
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun checkAndRequestCallScreeningRole(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    updateCallScreeningEnabled(true)
                } else {
                    _roleRequestTrigger.value = true
                }
            }
        } else {
            updateCallScreeningEnabled(true)
        }
    }

    private val _roleRequestTrigger = MutableStateFlow(false)
    val roleRequestTrigger: StateFlow<Boolean> = _roleRequestTrigger.asStateFlow()

    fun onRoleRequestHandled(isGranted: Boolean) {
        _roleRequestTrigger.value = false
        if (isGranted) {
            updateCallScreeningEnabled(true)
        }
    }

    private fun updateCallScreeningEnabled(enabled: Boolean) {
        persistCallScreeningEnabled(enabled)
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(isCallScreeningEnabled = enabled)
            } else currentState
        }
    }

    fun getStarted() {
        _uiState.value = MainUiState.CheckingPermission
        viewModelScope.launch {
            delay(800) // Small delay for UX
            val permission = com.example.scamguardv1.utils.PermissionHelper.audioPermission
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                refreshFiles()
            } else {
                _uiState.value = MainUiState.PermissionRequired
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            refreshFiles()
        } else {
            _uiState.value = MainUiState.PermissionDenied
        }
    }

    fun requestPermission() {
        _uiState.value = MainUiState.PermissionRequired
    }

    fun refreshFiles() {
        viewModelScope.launch {
            if (_uiState.value !is MainUiState.Success) {
                _uiState.value = MainUiState.Loading
            }
            
            getAudioFilesUseCase().onSuccess { files ->
                val deletedIds = getDeletedAudioFileIds()
                val filteredFiles = files.filter { it.id !in deletedIds }
                
                _uiState.update { latest ->
                    val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else {
                        true
                    }
                    
                    if (latest is MainUiState.Success) {
                        performCleanup(latest.copy(
                            audioFiles = filteredFiles,
                            isOverlayPermissionGranted = overlayGranted
                        ))
                    } else {
                        val success = MainUiState.Success(
                            audioFiles = filteredFiles,
                            isCallScreeningEnabled = isPersistedCallScreeningEnabled(),
                            isOverlayPermissionGranted = overlayGranted,
                            latestRiskScore = getPersistedLatestRiskScore(),
                            screenedCalls = getPersistedScreenedCalls(),
                            history = getPersistedScanHistory(),
                            currentTheme = getPersistedTheme(),
                            autoDeleteDays = getPersistedAutoDeleteDays()
                        )
                        performCleanup(success)
                    }
                }
                Logger.d("Successfully loaded ${filteredFiles.size} audio files")
            }.onFailure { e ->
                Logger.e("Failed to load audio files", e)
                _uiState.value = MainUiState.Error(Constants.ERROR_LOAD_FILES_FAILED)
            }
        }
    }

    fun scanFile(file: AudioFile) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(scanState = ScanUiState.Scanning(file))
            } else currentState
        }

        viewModelScope.launch {
            Logger.i("Starting scan for file: ${file.name}")
            
            analyzeVoiceUseCase(file).onSuccess { detectionResult ->
                val scoreInt = detectionResult.score.toInt()
                persistLatestRiskScore(scoreInt)
                _uiState.update { currentState ->
                    if (currentState is MainUiState.Success) {
                        val newHistory = currentState.history + (file.id to detectionResult)
                        persistScanHistory(newHistory)
                        currentState.copy(
                            scanState = ScanUiState.Success(file, detectionResult),
                            history = newHistory,
                            latestRiskScore = scoreInt,
                            selectedAudioFile = null
                        )
                    } else currentState
                }
                Logger.d("Scan completed for ${file.name}. Score: ${detectionResult.score}")
            }.onFailure { e ->
                Logger.e("Scan failed for ${file.name}", e)
                val errorMsg = e.message ?: Constants.ERROR_UNKNOWN
                _uiState.update { currentState ->
                    if (currentState is MainUiState.Success) {
                        currentState.copy(scanState = ScanUiState.Error(errorMsg))
                    } else currentState
                }
            }
        }
    }

    fun dismissResult() {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(
                    scanState = ScanUiState.Idle,
                    selectedAudioFile = null,
                    viewingResult = null
                )
            } else currentState
        }
    }

    fun showResult(title: String, result: DetectionResult) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(viewingResult = title to result)
            } else currentState
        }
    }

    fun deleteCallHistoryItem(call: ScreenedCall) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                val newList = currentState.screenedCalls.filter { it != call }
                persistScreenedCalls(newList)
                currentState.copy(screenedCalls = newList)
            } else currentState
        }
    }

    fun clearAllCallHistory() {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                persistScreenedCalls(emptyList())
                currentState.copy(screenedCalls = emptyList())
            } else currentState
        }
    }

    fun deleteScanResult(fileId: Long) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                val newHistory = currentState.history.filterKeys { it != fileId }
                persistScanHistory(newHistory)
                currentState.copy(history = newHistory)
            } else currentState
        }
    }

    private val _deletePermissionRequest = MutableStateFlow<android.app.PendingIntent?>(null)
    val deletePermissionRequest: StateFlow<android.app.PendingIntent?> = _deletePermissionRequest.asStateFlow()

    fun deleteAudioFile(file: AudioFile) {
        val deletedIds = getDeletedAudioFileIds().toMutableSet()
        deletedIds.add(file.id)
        persistDeletedAudioFileIds(deletedIds)
        
        // Also clean up scan history for this file
        deleteScanResult(file.id)
        
        refreshFiles()
    }

    fun onDeletePermissionHandled() {
        _deletePermissionRequest.value = null
        refreshFiles()
    }

    fun clearAllScanHistory() {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                persistScanHistory(emptyMap())
                currentState.copy(history = emptyMap())
            } else currentState
        }
    }

    fun selectAudioFile(file: AudioFile?) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                if (file == null) {
                    voicePlayer.stop()
                    stopPlaybackProgress()
                }
                currentState.copy(
                    selectedAudioFile = file,
                    isAudioPlaying = if (file == null) false else currentState.isAudioPlaying,
                    playingFileId = if (file == null) null else currentState.playingFileId
                )
            } else currentState
        }
    }

    fun setAudioScreeningViewVisible(visible: Boolean) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(isAudioScreeningViewVisible = visible)
            } else currentState
        }
    }

    fun setCallScreeningViewVisible(visible: Boolean) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(isCallScreeningViewVisible = visible)
            } else currentState
        }
    }

    fun setSettingsVisible(visible: Boolean) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(
                    isSettingsVisible = visible,
                    currentSettingsView = com.example.scamguardv1.ui.state.SettingsView.MAIN
                )
            } else currentState
        }
    }

    fun navigateSettingsTo(view: com.example.scamguardv1.ui.state.SettingsView) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(currentSettingsView = view)
            } else currentState
        }
    }

    fun updateTheme(theme: AppTheme) {
        persistTheme(theme)
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                currentState.copy(currentTheme = theme)
            } else currentState
        }
    }

    fun updateAutoDeleteDays(days: Int) {
        persistAutoDeleteDays(days)
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                performCleanup(currentState.copy(autoDeleteDays = days))
            } else currentState
        }
    }

    private fun performCleanup(state: MainUiState.Success): MainUiState.Success {
        val days = state.autoDeleteDays
        if (days <= 0) return state

        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        
        // Cleanup screened calls
        val cleanedCalls = state.screenedCalls.filter { it.timestamp >= cutoff }
        if (cleanedCalls.size != state.screenedCalls.size) {
            persistScreenedCalls(cleanedCalls)
            Logger.d("Auto-deleted ${state.screenedCalls.size - cleanedCalls.size} old calls")
        }

        // Cleanup scan history
        val cleanedHistory = state.history.filter { (id, _) ->
            val file = state.audioFiles.find { it.id == id }
            file == null || file.timestamp >= cutoff
        }
        if (cleanedHistory.size != state.history.size) {
            persistScanHistory(cleanedHistory)
            Logger.d("Auto-deleted ${state.history.size - cleanedHistory.size} old scan results")
        }

        return state.copy(
            screenedCalls = cleanedCalls,
            history = cleanedHistory
        )
    }

    private var playbackProgressJob: Job? = null

    fun playAudio(file: AudioFile) {
        _uiState.update { currentState ->
            if (currentState is MainUiState.Success) {
                val isCurrentlyPlayingThisFile = currentState.playingFileId == file.id
                
                if (isCurrentlyPlayingThisFile) {
                    if (currentState.isAudioPlaying) {
                        // Intent: Pause
                        voicePlayer.pause()
                        stopPlaybackProgress()
                        currentState.copy(isAudioPlaying = false)
                    } else {
                        // Intent: Resume
                        voicePlayer.play()
                        startPlaybackProgress()
                        currentState.copy(isAudioPlaying = true)
                    }
                } else {
                    // Intent: Play new file
                    voicePlayer.stop() // Stop previous if any
                    voicePlayer.prepare(file.uri) {
                        _uiState.update { state ->
                            if (state is MainUiState.Success && state.playingFileId == file.id) {
                                stopPlaybackProgress()
                                state.copy(playingFileId = null, playbackPosition = 0L, isAudioPlaying = false)
                            } else state
                        }
                    }
                    voicePlayer.play()
                    startPlaybackProgress()
                    currentState.copy(
                        playingFileId = file.id,
                        isAudioPlaying = true,
                        playbackDuration = file.duration.toLong(),
                        playbackPosition = 0L
                    )
                }
            } else currentState
        }
    }

    fun seekTo(positionMs: Long) {
        voicePlayer.seekTo(positionMs)
        _uiState.update { state ->
            if (state is MainUiState.Success) {
                state.copy(playbackPosition = positionMs)
            } else state
        }
    }

    private fun startPlaybackProgress() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (isActive) {
                val current = voicePlayer.getCurrentPosition()
                _uiState.update { state ->
                    if (state is MainUiState.Success) {
                        state.copy(playbackPosition = current)
                    } else state
                }
                delay(500)
            }
        }
    }

    private fun stopPlaybackProgress() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.release()
    }
}
