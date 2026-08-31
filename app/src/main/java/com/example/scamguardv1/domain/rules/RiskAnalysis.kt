package com.example.scamguardv1.domain.rules

import androidx.compose.ui.graphics.Color
import com.example.scamguardv1.utils.Constants

enum class RiskLevel(
    val label: String,
    val color: Color,
    val explanation: String,
    val recommendation: String
) {
    HIGH(
        "HIGH RISK",
        Color(0xFFD32F2F),
        "Significant patterns of voice manipulation detected. This audio is likely AI-generated or altered.",
        "Do not trust this audio. Avoid sharing sensitive information or performing any financial transactions based on it."
    ),
    MEDIUM(
        "MEDIUM RISK",
        Color(0xFFFBC02D),
        "Some anomalies detected. The audio might be processed or low quality, causing uncertainty.",
        "Proceed with caution. Verify the identity of the speaker through another trusted channel before taking action."
    ),
    LOW(
        "LOW RISK",
        Color(0xFF388E3C),
        "No significant manipulation detected. The voice patterns appear natural and authentic.",
        "The audio appears safe, but always stay vigilant against social engineering tactics."
    )
}

object RiskAnalysis {
    fun getRiskLevel(score: Float): RiskLevel {
        return when {
            score > Constants.HIGH_RISK_THRESHOLD -> RiskLevel.HIGH
            score > Constants.MEDIUM_RISK_THRESHOLD -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
}
