package com.example.scamguardv1.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamguardv1.ui.composables.VerityLogo
import java.util.Locale

@Composable
fun VoiceRecordingScreen(
    userEmail: String?,
    isRecording: Boolean,
    elapsedSeconds: Int,
    errorMessage: String?,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onSignOut: () -> Unit
) {
    val totalRequiredSeconds = 120 // 2 minutes
    val progress = elapsedSeconds.toFloat() / totalRequiredSeconds.toFloat()
    
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    // Pulsing mic effect when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            VerityLogo(modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "SECURE VOICE PROFILE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3478F6),
                letterSpacing = 2.sp
            )
            Text(
                text = userEmail ?: "New User",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Baseline Voice Enrollment Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "To protect your identity from AI voice clones and deepfake telemarketing scams, Verity requires a necessary 2-minute in-app baseline voice recording saved securely in high-fidelity MP3 format.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            // Pulse Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .background(Color(0xFF3478F6).copy(alpha = 0.15f), shape = CircleShape)
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) Color(0xFFEB5757) else Color(0xFF3478F6),
                    modifier = Modifier.size(100.dp),
                    shadowElevation = 6.dp
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Pause" else "Record",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = if (isRecording) Color(0xFFEB5757) else MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "REQUIRED TIME: 02:00",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(28.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(5.dp)),
                color = Color(0xFF3478F6)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}% Completed",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3478F6)
            )

            errorMessage?.let { err ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEB5757).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEB5757))
                        Spacer(Modifier.width(8.dp))
                        Text(text = err, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEB5757))
                    }
                }
            }
        }

        // Bottom Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { if (isRecording) onPauseRecording() else onStartRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFEB5757) else Color(0xFF3478F6)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "PAUSE RECORDING" else "START AUDIO ENROLLMENT",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onSignOut) {
                Text(text = "Sign Out & Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
