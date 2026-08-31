package com.example.scamguardv1.ui.composables

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import com.example.scamguardv1.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamguardv1.domain.model.AudioFile
import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.domain.rules.RiskAnalysis
import androidx.compose.ui.platform.LocalContext
import com.example.scamguardv1.utils.PdfGenerator
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResultPanel(fileName: String, result: DetectionResult, onClose: () -> Unit) {
    val risk = remember(result.score) { RiskAnalysis.getRiskLevel(result.score) }
    var showDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = if (showDetails) Alignment.Start else Alignment.CenterHorizontally
    ) {
        if (!showDetails) {
            // Main Result View
            item(key = "header") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Analysis Result",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            item(key = "score_circle") {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { result.score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = risk.color,
                        strokeWidth = 10.dp,
                        trackColor = risk.color.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${result.score.toInt()}%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = risk.color
                        )
                        Text(
                            "SUSPECT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = risk.color,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
            
            item(key = "risk_label") {
                Spacer(Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        risk.label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = risk.color
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        risk.explanation,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            item(key = "recommendation") {
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = risk.color.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, risk.color.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Recommendation",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = risk.color
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = risk.recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "reasons_title") {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Detection Reasons",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            itemsIndexed(result.reasons) { index, reason ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = risk.color, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item(key = "main_buttons") {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { showDetails = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("View Pattern Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(Modifier.height(12.dp))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(32.dp))
            }
        } else {
            // Pattern Analysis View
            item(key = "details_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { showDetails = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column {
                        Text(
                            "Pattern Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text(
                    "Technical breakdown of voice characteristics analyzed by ML models.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "features_title") {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Voice Characteristics",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = risk.color
                )
                Spacer(Modifier.height(8.dp))
            }

            result.features?.let { features ->
                item(key = "feat_spectral") { FeatureItem("Spectral Centroid", features.spectralCentroid, "Measures the 'brightness' of the sound. Cloned voices often have unnatural peaks.") }
                item(key = "feat_zcr") { FeatureItem("Zero Crossing Rate", features.zeroCrossingRate, "Detects high-frequency noise artifacts common in synthetic audio generation.") }
                item(key = "feat_stability") { FeatureItem("Energy Stability", features.energyStability, "Natural speech has high variance; robotic voices are often too consistent.") }
                item(key = "feat_harmonics") { FeatureItem("Harmonics Ratio", features.harmonicsRatio, "Measures the clarity of vocal tones versus background noise artifacts.") }
            } ?: item(key = "feat_none") {
                Text("Technical data not available for this scan.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            item(key = "details_buttons") {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        scope.launch {
                            PdfGenerator.generateAnalysisReport(context, fileName, result)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Download Full Report (PDF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FeatureItem(label: String, value: Float, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("%.2f".format(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF3478F6))
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (value % 1f) }, // Just for visualization since actual bounds vary
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = Color(0xFF3478F6),
            trackColor = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun VerityLogo(modifier: Modifier = Modifier) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.ic_logo_animated)
    var atEnd by remember { mutableStateOf(false) }
    
    // Auto-animate loop
    LaunchedEffect(Unit) {
        while (true) {
            atEnd = !atEnd
            delay(1500)
        }
    }
    
    val painter = rememberAnimatedVectorPainter(image, atEnd)
    Image(
        painter = painter,
        contentDescription = "Verity Logo",
        modifier = modifier
    )
}

@Composable
fun BrandSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VerityLogo(modifier = Modifier.size(120.dp))
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "VERITY",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 8.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "KNOW BEFORE YOU TRUST",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3478F6),
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { 20 })
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VerityLogo(modifier = Modifier.size(140.dp))
                
                Spacer(Modifier.height(48.dp))
                
                Text(
                    "VERITY",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 6.sp
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "Secure your voice, protect your trust.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                
                Spacer(Modifier.height(80.dp))
                
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6)),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "GET STARTED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String = "Scanning audio files...") {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF3478F6),
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GlobalErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFEB5757)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Oops! Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6))
        ) {
            Text("Retry", color = Color.White)
        }
    }
}

@Composable
fun PermissionScreen(
    isDenied: Boolean,
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(if (isDenied) Color(0xFFEB5757).copy(alpha = 0.2f) else Color(0xFF3478F6).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isDenied) Icons.Default.Warning else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isDenied) Color(0xFFEB5757) else Color(0xFF3478F6)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            text = if (isDenied) "Permission Denied" else "Storage Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = if (isDenied) {
                "We cannot scan your audio files without storage access. Please enable it in Settings or try again."
            } else {
                "Verity needs access to your audio files to analyze them for voice manipulation and protect you from scams."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                if (isDenied) "Grant Access" else "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        if (isDenied) {
            TextButton(
                onClick = onGrantClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Try Again", color = Color(0xFF3478F6))
            }
        }
    }
}
