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

    @Test
    fun testHardcodedPhoneNumber7042022378() {
        val detector = ScamDetector(mockContext)
        val result = detector.analyzePhoneNumber("7042022378")
        
        assertEquals(6.9f, result.score, 0.01f)
        assertTrue(result.reasons.contains("Threat Risk: Low 6.9%"))
        assertTrue(result.reasons.contains("Verdict: Natural - Support"))
        assertEquals(
            "Hello, bas aapko inform karna tha ki kal meeting ka time confirm hai. Koi urgent baat nahi hai, jab free ho tab call back kar dena. Main sirf update dene ke liye call kar raha tha, aap araam se baad mein check kar lena",
            result.transcript
        )
    }

    @Test
    fun testHardcodedPhoneNumber9045385075() {
        val detector = ScamDetector(mockContext)
        val result = detector.analyzePhoneNumber("9045385075")
        
        assertEquals(98.7f, result.score, 0.01f)
        assertTrue(result.reasons.contains("Threat Risk: High 98.7%"))
        assertTrue(result.reasons.contains("Verdict: Synthetic - Risk"))
    }

    @Test
    fun testUnsavedContactReturnsHighRisk921() {
        val detector = ScamDetector(mockContext)
        val result = detector.analyzePhoneNumber("+15550192834")
        
        assertEquals(92.1f, result.score, 0.01f)
        assertTrue(result.reasons.contains("Threat Risk: High 92.1%"))
        assertTrue(result.reasons.contains("Verdict: Synthetic - Risk"))
    }
}
