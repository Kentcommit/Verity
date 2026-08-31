package com.example.scamguardv1

import android.content.Context
import com.example.scamguardv1.data.VoiceAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VoiceAnalyzerTest {

    @Mock
    lateinit var mockContext: Context

    @Test
    fun testExtractFeaturesFromEmptyData() {
        val analyzer = VoiceAnalyzer(mockContext)
        val features = analyzer.extractFeatures(byteArrayOf())
        
        assertEquals(0f, features.spectralCentroid)
        assertEquals(0f, features.zeroCrossingRate)
        assertEquals(0f, features.energyStability)
    }

    @Test
    fun testExtractFeaturesFromSineWave() {
        val analyzer = VoiceAnalyzer(mockContext)
        // Simulate a simple 16-bit PCM buffer (square wave for simplicity)
        val audioData = ByteArray(1000) { if (it % 4 < 2) 0xFF.toByte() else 0x00.toByte() }
        
        val features = analyzer.extractFeatures(audioData)
        
        assertNotNull(features)
        assertTrue("ZCR should be positive for varying signals", features.zeroCrossingRate > 0)
    }
}
