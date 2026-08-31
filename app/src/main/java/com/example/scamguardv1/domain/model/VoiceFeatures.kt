package com.example.scamguardv1.domain.model

data class VoiceFeatures(
    val spectralCentroid: Float,
    val zeroCrossingRate: Float,
    val energyStability: Float,
    val harmonicsRatio: Float
)
