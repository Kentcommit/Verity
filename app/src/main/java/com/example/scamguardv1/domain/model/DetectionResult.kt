package com.example.scamguardv1.domain.model

data class DetectionResult(
    val score: Float,
    val reasons: List<String>,
    val features: VoiceFeatures? = null
)
