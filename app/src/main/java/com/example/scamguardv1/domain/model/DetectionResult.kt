package com.example.scamguardv1.domain.model

data class DetectionResult(
    val score: Float,
    val reasons: List<String>,
    val voiceFeatures: VoiceFeatures? = null,
    val urlFeatures: UrlFeatures? = null,
    val transcript: String? = null
) {
    val prediction: String
        get() = if (score > 40) "malicious" else "benign"
}

