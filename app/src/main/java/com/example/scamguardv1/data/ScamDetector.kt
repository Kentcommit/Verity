package com.example.scamguardv1.data

import android.content.Context

import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.utils.Constants

class ScamDetector(private val context: Context) {

    /**
     * Analyzes VoiceFeatures using a heuristic model to detect potential manipulation.
     */
    fun analyzeVoice(features: VoiceFeatures): DetectionResult {
        var suspicionScore = 0f
        val reasons = mutableListOf<String>()

        // Robotic consistency check (Energy Stability)
        if (features.energyStability > Constants.STABILITY_THRESHOLD_HIGH) {
            suspicionScore += Constants.WEIGHT_ENERGY_STABILITY_HIGH
            reasons.add("Abnormal energy stability detected (lack of natural variance)")
        } else if (features.energyStability > Constants.STABILITY_THRESHOLD_MEDIUM) {
            suspicionScore += Constants.WEIGHT_ENERGY_STABILITY_MEDIUM
            reasons.add("Slightly elevated energy consistency")
        }

        // Spectral Centroid analysis
        if (features.spectralCentroid in Constants.CENTROID_BOUND_LOW..Constants.CENTROID_BOUND_HIGH) {
            suspicionScore += Constants.WEIGHT_SPECTRAL_CENTROID
            reasons.add("Spectral signature matches common voice cloning patterns")
        }

        // Zero Crossing Rate (ZCR) anomalies
        if (features.zeroCrossingRate < Constants.ZCR_BOUND_LOW) {
            suspicionScore += Constants.WEIGHT_ZCR_ANOMALY
            reasons.add("Unusually low noise floor detected")
        } else if (features.zeroCrossingRate > Constants.ZCR_BOUND_HIGH) {
            suspicionScore += Constants.WEIGHT_ZCR_ANOMALY
            reasons.add("High frequency artifacts detected")
        }

        if (reasons.isEmpty()) {
            reasons.add("Voice patterns appear within natural human ranges")
        }

        val noise = (0..5).random().toFloat()
        val finalScore = (suspicionScore + noise).coerceIn(0f, 100f)

        return DetectionResult(finalScore, reasons, features)
    }
}
