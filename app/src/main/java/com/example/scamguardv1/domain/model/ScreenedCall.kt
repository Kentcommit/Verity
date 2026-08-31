package com.example.scamguardv1.domain.model

data class ScreenedCall(
    val phoneNumber: String,
    val score: Float,
    val reasons: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
