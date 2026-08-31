package com.example.scamguardv1

import android.content.Context
import com.example.scamguardv1.data.ScamDetector
import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScamDetectorTest {

    @Mock
    lateinit var mockContext: Context

    @Test
    fun testAnalyzeVoiceReturnsValidScore() {
        val detector = ScamDetector(mockContext)
        val features = VoiceFeatures(0.5f, 0.2f, 0.9f, 0.4f)
        val result = detector.analyzeVoice(features)
        
        assertTrue("Score should be between 0 and 100", result.score in 0f..100f)
    }

    @Test
    fun testHighEnergyStabilityTriggersHighRisk() {
        val detector = ScamDetector(mockContext)
        // Set stability above threshold
        val features = VoiceFeatures(0.1f, 0.1f, Constants.STABILITY_THRESHOLD_HIGH + 0.01f, 0.1f)
        val result = detector.analyzeVoice(features)
        
        assertTrue("Score should reflect high risk weights", result.score >= Constants.WEIGHT_ENERGY_STABILITY_HIGH)
        assertTrue(result.reasons.any { it.contains("energy stability", ignoreCase = true) })
    }

    @Test
    fun testLowRiskFeaturesReturnLowScore() {
        val detector = ScamDetector(mockContext)
        // Features outside all suspicion bands
        val features = VoiceFeatures(0.1f, 0.2f, 0.3f, 0.1f)
        val result = detector.analyzeVoice(features)
        
        assertTrue("Score should be very low", result.score < 10f)
        assertEquals("Voice patterns appear within natural human ranges", result.reasons[0])
    }
}
