package com.example.scamguardv1.ui.screens

import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.scamguardv1.domain.model.AudioFile
import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.ui.MainViewModel
import com.example.scamguardv1.ui.composables.*
import com.example.scamguardv1.ui.state.AppTheme
import com.example.scamguardv1.ui.state.MainUiState
import com.example.scamguardv1.ui.state.ScanUiState
import com.example.scamguardv1.ui.state.SettingsView
import com.example.scamguardv1.ui.theme.ScamGuardV1Theme
import com.example.scamguardv1.utils.Constants
import com.example.scamguardv1.utils.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ActivityItem {
    data class Call(val call: com.example.scamguardv1.domain.model.ScreenedCall) : ActivityItem()
    data class Scan(val file: AudioFile, val result: DetectionResult) : ActivityItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamGuardApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val roleRequestTrigger by viewModel.roleRequestTrigger.collectAsState()
    val deletePermissionRequest by viewModel.deletePermissionRequest.collectAsState()

    val currentTheme = (uiState as? MainUiState.Success)?.currentTheme ?: AppTheme.SYSTEM
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = remember(currentTheme, isSystemDark) {
        when (currentTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> isSystemDark
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    val resultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResultSheet by remember { mutableStateOf(false) }

    val viewResultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showViewResultSheet by remember { mutableStateOf(false) }

    val settingsSheetState = rememberModalBottomSheetState()

    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    val currentState = uiState as? MainUiState.Success
    val scanState = currentState?.scanState
    val viewingResult = currentState?.viewingResult
    val selectedAudioFile = currentState?.selectedAudioFile

    LaunchedEffect(selectedAudioFile?.id) {
        showSheet = selectedAudioFile != null
    }

    LaunchedEffect(scanState) {
        showResultSheet = scanState is ScanUiState.Success
    }

    LaunchedEffect(viewingResult) {
        showViewResultSheet = viewingResult != null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeletePermissionHandled()
        }
    }

    LaunchedEffect(deletePermissionRequest) {
        deletePermissionRequest?.let { pendingIntent ->
            deleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build()
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onRoleRequestHandled(isGranted = (result.resultCode == android.app.Activity.RESULT_OK))
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importAudioFile(uri)
        }
    }

    // Stable lambdas to prevent unnecessary recompositions
    val onBackAudioScreening = remember { { viewModel.setAudioScreeningViewVisible(false) } }
    val onImportAudio = remember { { audioPickerLauncher.launch("audio/*") } }
    val onSelectFile = remember { { file: AudioFile -> viewModel.selectAudioFile(file) } }
    val onViewResult = remember { { title: String, result: DetectionResult -> viewModel.showResult(title, result) } }
    val onClearAllScan = remember { { viewModel.clearAllScanHistory() } }
    val onDeleteRecording = remember { { file: AudioFile -> itemToDelete = file } }

    val onBackCallScreening = remember { { viewModel.setCallScreeningViewVisible(false) } }
    val onToggleCallScreening = remember { { viewModel.toggleCallScreening(context) } }
    val onClearAllCalls = remember { { viewModel.clearAllCallHistory() } }
    val onDeleteCall = remember { { call: com.example.scamguardv1.domain.model.ScreenedCall -> itemToDelete = call } }

    DisposableEffect(Unit) {
        viewModel.registerReceiver(context)
        onDispose {
            viewModel.unregisterReceiver(context)
        }
    }

    LaunchedEffect(roleRequestTrigger) {
        if (roleRequestTrigger && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkOverlayPermission(context)
    }

    ScamGuardV1Theme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                when (val state = uiState) {
                    is MainUiState.Splash -> BrandSplashScreen()
                    is MainUiState.Welcome -> WelcomeScreen(onGetStarted = { viewModel.getStarted() })
                    is MainUiState.CheckingPermission -> LoadingScreen("Checking permissions...")
                    is MainUiState.PermissionRequired -> PermissionScreen(isDenied = false, onGrantClick = { launcher.launch(PermissionHelper.audioPermission) })
                    is MainUiState.PermissionDenied -> PermissionScreen(isDenied = true, onGrantClick = { launcher.launch(PermissionHelper.audioPermission) })
                    is MainUiState.Loading -> LoadingScreen("Loading...")
                    is MainUiState.Error -> GlobalErrorScreen(message = state.message, onRetry = { viewModel.refreshFiles() })
                    is MainUiState.Success -> {
                        // Handle System Back Button
                        BackHandler(enabled = state.isAudioScreeningViewVisible || state.isCallScreeningViewVisible || state.isSettingsVisible || state.selectedAudioFile != null || state.viewingResult != null || state.scanState is ScanUiState.Success) {
                            when {
                                state.viewingResult != null -> viewModel.dismissResult()
                                state.scanState is ScanUiState.Success -> viewModel.dismissResult()
                                state.isSettingsVisible -> {
                                    if (state.currentSettingsView != SettingsView.MAIN) {
                                        viewModel.navigateSettingsTo(SettingsView.MAIN)
                                    } else {
                                        viewModel.setSettingsVisible(false)
                                    }
                                }
                                state.selectedAudioFile != null -> viewModel.selectAudioFile(null)
                                state.isAudioScreeningViewVisible -> viewModel.setAudioScreeningViewVisible(false)
                                state.isCallScreeningViewVisible -> viewModel.setCallScreeningViewVisible(false)
                            }
                        }

                        val recentActivities = remember(state.screenedCalls, state.history, state.audioFiles) {
                            val calls = state.screenedCalls.map { ActivityItem.Call(it) }
                            val scans = state.history.mapNotNull { (id, result) ->
                                val file = state.audioFiles.find { it.id == id }
                                if (file != null) ActivityItem.Scan(file, result) else null
                            }
                            (calls + scans).sortedByDescending { 
                                when (it) {
                                    is ActivityItem.Call -> it.call.timestamp
                                    is ActivityItem.Scan -> it.file.timestamp
                                }
                            }.take(3)
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF3478F6).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            VerityLogo(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Verity",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            "Know Before You Trust",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.setSettingsVisible(true) }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    RiskScoreCard(score = state.latestRiskScore)
                                }

                                item {
                                    Text(
                                        "Protection tools",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (darkTheme) Color.White else Color.Black
                                    )
                                }

                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            ProtectionToolItem(
                                                title = "Audio Screening",
                                                description = "Analyze recordings",
                                                icon = Icons.Default.Mic,
                                                iconColor = if (darkTheme) Color.White else Color.Black,
                                                onClick = { viewModel.setAudioScreeningViewVisible(true) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProtectionToolItem(
                                                title = "Payment ID",
                                                description = "Check UPI IDs",
                                                icon = Icons.Default.CreditCard,
                                                iconColor = Color(0xFF3478F6),
                                                onClick = { /* TODO */ },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            ProtectionToolItem(
                                                title = "Call Screening",
                                                description = "Identify flag",
                                                icon = Icons.Default.Call,
                                                iconColor = Color(0xFFEB5757),
                                                onClick = { viewModel.setCallScreeningViewVisible(true) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProtectionToolItem(
                                                title = "QR Check",
                                                description = "Inspect QR",
                                                icon = Icons.Default.PhotoCamera,
                                                iconColor = Color.Gray,
                                                onClick = { /* TODO */ },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        "Recent protection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (darkTheme) Color.White else Color.Black
                                    )
                                }

                                items(
                                    recentActivities,
                                    key = { 
                                        when (it) {
                                            is ActivityItem.Call -> "call_${it.call.timestamp}"
                                            is ActivityItem.Scan -> "scan_${it.file.id}"
                                        }
                                    }
                                ) { activity ->
                                    when (activity) {
                                        is ActivityItem.Call -> {
                                            val call = activity.call
                                            RecentProtectionItem(
                                                title = "Unknown caller screened",
                                                subtitle = "Today • ${if (call.score > 70) "High risk" else "Low risk"}",
                                                score = call.score.toInt(),
                                                icon = Icons.Default.Call,
                                                iconColor = Color(0xFFEB5757),
                                                onInfoClick = {
                                                    viewModel.showResult(
                                                        call.phoneNumber,
                                                        DetectionResult(call.score, call.reasons)
                                                    )
                                                },
                                                onLongClick = { itemToDelete = call }
                                            )
                                        }
                                        is ActivityItem.Scan -> {
                                            val file = activity.file
                                            val result = activity.result
                                            RecentProtectionItem(
                                                title = "Call recording analyzed",
                                                subtitle = file.name,
                                                score = result.score.toInt(),
                                                icon = Icons.Default.Mic,
                                                iconColor = if (darkTheme) Color.White else Color.Black,
                                                onInfoClick = {
                                                    viewModel.showResult(file.name, result)
                                                },
                                                onLongClick = { itemToDelete = file.id }
                                            )
                                        }
                                    }
                                }
                            }

                            if (state.isSettingsVisible) {
                                ModalBottomSheet(
                                    onDismissRequest = { viewModel.setSettingsVisible(false) },
                                    sheetState = settingsSheetState,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    SettingsScreen(
                                        currentView = state.currentSettingsView,
                                        currentTheme = state.currentTheme,
                                        autoDeleteDays = state.autoDeleteDays,
                                        onBack = {
                                            if (state.currentSettingsView != SettingsView.MAIN) {
                                                viewModel.navigateSettingsTo(SettingsView.MAIN)
                                            } else {
                                                viewModel.setSettingsVisible(false)
                                            }
                                        },
                                        onNavigateToTheme = { viewModel.navigateSettingsTo(SettingsView.THEME) },
                                        onNavigateToAutoDelete = { viewModel.navigateSettingsTo(SettingsView.AUTO_DELETE) },
                                        onThemeChange = { viewModel.updateTheme(it) },
                                        onAutoDeleteChange = { viewModel.updateAutoDeleteDays(it) },
                                        darkTheme = darkTheme
                                    )
                                }
                            }

                            if (showSheet && state.selectedAudioFile != null) {
                                RecordingDetailsSheet(
                                    isVisible = true,
                                    file = state.selectedAudioFile,
                                    isPlaying = state.isAudioPlaying && state.playingFileId == state.selectedAudioFile.id,
                                    isScanning = (state.scanState as? ScanUiState.Scanning)?.file?.id == state.selectedAudioFile.id,
                                    playbackPosition = state.playbackPosition,
                                    playbackDuration = state.playbackDuration,
                                    hasResult = state.history.containsKey(state.selectedAudioFile.id),
                                    darkTheme = darkTheme,
                                    sheetState = sheetState,
                                    onDismiss = { viewModel.selectAudioFile(null) },
                                    onPlay = { viewModel.playAudio(state.selectedAudioFile) },
                                    onAnalyze = { viewModel.scanFile(state.selectedAudioFile) },
                                    onSeek = { viewModel.seekTo(it) },
                                    onDeleteResult = { viewModel.deleteScanResult(state.selectedAudioFile.id) }
                                )
                            }

                            if (showResultSheet && scanState is ScanUiState.Success) {
                                ResultSheet(
                                    isVisible = true,
                                    fileName = scanState.file.name,
                                    result = scanState.result,
                                    darkTheme = darkTheme,
                                    sheetState = resultSheetState,
                                    onDismiss = { viewModel.dismissResult() }
                                )
                            }

                            if (showViewResultSheet && viewingResult != null) {
                                ResultSheet(
                                    isVisible = true,
                                    fileName = viewingResult.first,
                                    result = viewingResult.second,
                                    darkTheme = darkTheme,
                                    sheetState = viewResultSheetState,
                                    onDismiss = { viewModel.dismissResult() }
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.isAudioScreeningViewVisible,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            AudioScreeningScreen(
                                audioFiles = state.audioFiles,
                                history = state.history,
                                onBack = onBackAudioScreening,
                                onImport = onImportAudio,
                                onSelectFile = onSelectFile,
                                onViewResult = onViewResult,
                                onClearAll = onClearAllScan,
                                onDeleteRecording = onDeleteRecording
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.isCallScreeningViewVisible,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            CallScreeningHistoryScreen(
                                calls = state.screenedCalls,
                                isEnabled = state.isCallScreeningEnabled,
                                onBack = onBackCallScreening,
                                onToggle = onToggleCallScreening,
                                onViewResult = onViewResult,
                                onClearAll = onClearAllCalls,
                                onDeleteCall = onDeleteCall
                            )
                        }

                        if (itemToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { itemToDelete = null },
                                title = { Text("Delete Entry?", color = MaterialTheme.colorScheme.onSurface) },
                                text = { 
                                    Text(
                                        when (itemToDelete) {
                                            is com.example.scamguardv1.domain.model.ScreenedCall -> "Are you sure you want to remove this call from your history?"
                                            is AudioFile -> "Are you sure you want to remove this recording from Verity?"
                                            else -> "Confirm deletion?"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            when (val item = itemToDelete) {
                                                is com.example.scamguardv1.domain.model.ScreenedCall -> {
                                                    viewModel.deleteCallHistoryItem(item)
                                                }
                                                is AudioFile -> {
                                                    viewModel.deleteAudioFile(item)
                                                }
                                                is Long -> {
                                                    viewModel.deleteScanResult(item)
                                                }
                                            }
                                            itemToDelete = null
                                        }
                                    ) {
                                        Text("Delete", color = Color(0xFFEB5757))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { itemToDelete = null }) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    isVisible: Boolean,
    fileName: String,
    result: DetectionResult,
    darkTheme: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ) {
            ResultPanel(
                fileName = fileName,
                result = result,
                onClose = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailsSheet(
    isVisible: Boolean,
    file: AudioFile,
    isPlaying: Boolean,
    isScanning: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    hasResult: Boolean,
    darkTheme: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAnalyze: () -> Unit,
    onSeek: (Long) -> Unit,
    onDeleteResult: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ) {
            RecordingDetailsContent(
                file = file,
                isPlaying = isPlaying,
                isScanning = isScanning,
                playbackPosition = playbackPosition,
                playbackDuration = playbackDuration,
                hasResult = hasResult,
                onPlay = onPlay,
                onAnalyze = onAnalyze,
                onSeek = onSeek,
                onDeleteResult = onDeleteResult
            )
        }
    }
}

@Composable
fun SettingsScreen(
    currentView: SettingsView,
    currentTheme: AppTheme,
    autoDeleteDays: Int,
    onBack: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAutoDelete: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onAutoDeleteChange: (Int) -> Unit,
    darkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = when (currentView) {
                    SettingsView.MAIN -> "Settings"
                    SettingsView.THEME -> "Theme"
                    SettingsView.AUTO_DELETE -> "Auto-delete History"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Main Settings View
            androidx.compose.animation.AnimatedVisibility(
                visible = currentView == SettingsView.MAIN,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingMenuItem(
                        title = "Appearance",
                        subtitle = when (currentTheme) {
                            AppTheme.LIGHT -> "Light"
                            AppTheme.DARK -> "Dark"
                            AppTheme.SYSTEM -> "System default"
                        },
                        icon = Icons.Default.Palette,
                        onClick = onNavigateToTheme,
                        darkTheme = darkTheme
                    )
                    
                    SettingMenuItem(
                        title = "Auto-delete history",
                        subtitle = if (autoDeleteDays == 0) "Never" else "After $autoDeleteDays days",
                        icon = Icons.Default.DeleteSweep,
                        onClick = onNavigateToAutoDelete,
                        darkTheme = darkTheme
                    )
                }
            }

            // Theme Selection View
            androidx.compose.animation.AnimatedVisibility(
                visible = currentView == SettingsView.THEME,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ThemeOption(
                        title = "Light",
                        selected = currentTheme == AppTheme.LIGHT,
                        onClick = { onThemeChange(AppTheme.LIGHT) },
                        darkTheme = darkTheme
                    )
                    ThemeOption(
                        title = "Dark",
                        selected = currentTheme == AppTheme.DARK,
                        onClick = { onThemeChange(AppTheme.DARK) },
                        darkTheme = darkTheme
                    )
                    ThemeOption(
                        title = "System Default",
                        selected = currentTheme == AppTheme.SYSTEM,
                        onClick = { onThemeChange(AppTheme.SYSTEM) },
                        darkTheme = darkTheme
                    )
                }
            }

            // Auto-delete Selection View
            androidx.compose.animation.AnimatedVisibility(
                visible = currentView == SettingsView.AUTO_DELETE,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val options = listOf(0, 7, 30, 90)
                    options.forEach { days ->
                        ThemeOption(
                            title = if (days == 0) "Never" else "After $days days",
                            selected = autoDeleteDays == days,
                            onClick = { onAutoDeleteChange(days) },
                            darkTheme = darkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF3478F6),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3478F6))
        )
    }
}

@Composable
fun RecordingDetailsContent(
    file: AudioFile,
    isPlaying: Boolean,
    isScanning: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    hasResult: Boolean,
    onPlay: () -> Unit,
    onAnalyze: () -> Unit,
    onSeek: (Long) -> Unit,
    onDeleteResult: () -> Unit
) {
    val minutes = (file.duration / 1000) / 60
    val seconds = (file.duration / 1000) % 60
    val durationText = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val importedDate = remember(file.timestamp) { dateFormatter.format(Date(file.timestamp)) }

    val currentMinutes = (playbackPosition / 1000) / 60
    val currentSeconds = (playbackPosition / 1000) % 60
    val currentPositionText = String.format(Locale.getDefault(), "%d:%02d", currentMinutes, currentSeconds)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp)) // To center the icon
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(
                onClick = onDeleteResult,
                enabled = hasResult,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Score",
                    tint = if (hasResult) Color(0xFFEB5757) else Color.DarkGray
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            file.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Duration: $durationText • Imported: $importedDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))

        // Playback Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = playbackPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(if (playbackDuration > 0) playbackDuration.toFloat() else 1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3478F6),
                    activeTrackColor = Color(0xFF3478F6),
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(currentPositionText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(durationText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Pause" else "Play")
            }
            
            Button(
                onClick = onAnalyze,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5757)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze")
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AudioScreeningScreen(
    audioFiles: List<AudioFile>,
    history: Map<Long, DetectionResult>,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onSelectFile: (AudioFile) -> Unit,
    onViewResult: (String, DetectionResult) -> Unit,
    onClearAll: () -> Unit,
    onDeleteRecording: (AudioFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Audio Screening",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear Scores", color = Color(0xFFEB5757), style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = "Import", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        if (audioFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No recordings found", color = Color.Gray)
                    TextButton(onClick = onImport) {
                        Text("Import Recording", color = Color(0xFF3478F6))
                    }
                }
            }
        } else {
            val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(audioFiles, key = { it.id }) { file ->
                    val previousResult = history[file.id]
                    val dateStr = remember(file.timestamp) { dateFormatter.format(Date(file.timestamp)) }
                    
                    RecentProtectionItem(
                        title = file.name,
                        subtitle = "$dateStr • ${if (previousResult != null) "Analyzed" else "Not analyzed"}",
                        score = previousResult?.score?.toInt() ?: 0,
                        icon = Icons.Default.Mic,
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { onSelectFile(file) },
                        onInfoClick = if (previousResult != null) {
                            { onViewResult(file.name, previousResult) }
                        } else null,
                        onLongClick = { onDeleteRecording(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallScreeningHistoryScreen(
    calls: List<com.example.scamguardv1.domain.model.ScreenedCall>,
    isEnabled: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onViewResult: (String, DetectionResult) -> Unit,
    onClearAll: () -> Unit,
    onDeleteCall: (com.example.scamguardv1.domain.model.ScreenedCall) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Call Screening",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            if (calls.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = Color(0xFFEB5757), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProtectionActiveToggle(
                    isEnabled = isEnabled,
                    onToggle = { onToggle() }
                )
            }

            if (calls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No calls screened yet",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Call History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(calls, key = { it.timestamp }) { call ->
                    RecentProtectionItem(
                        title = call.phoneNumber,
                        subtitle = "Screened • ${dateFormatter.format(Date(call.timestamp))}",
                        score = call.score.toInt(),
                        icon = Icons.Default.Call,
                        iconColor = Color(0xFFEB5757),
                        onInfoClick = {
                            onViewResult(
                                call.phoneNumber,
                                DetectionResult(call.score, call.reasons)
                            )
                        },
                        onLongClick = { onDeleteCall(call) }
                    )
                }
            }
        }
    }
}
