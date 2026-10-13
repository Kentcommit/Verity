package com.example.scamguardv1

import com.example.scamguardv1.domain.rules.UrlRiskAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlRiskAnalyzerTest {

    @Test
    fun ordinaryHttpsUrlRemainsLowerRisk() {
        val result = UrlRiskAnalyzer.analyze("https://example.com")

        assertEquals("benign", result.prediction)
        assertTrue(result.score < 20) // Only HTTPS is 0, if HTTP it would be 15
    }

    @Test
    fun phishingLikeUrlIsNotReportedAsSafe() {
        // http (15) + secure (5) + login (5) + verify (5) + account (5) + .xyz (15) = 50
        val result = UrlRiskAnalyzer.analyze("http://secure-login-example.xyz/verify/account")

        assertEquals("malicious", result.prediction)
        assertEquals(50f, result.score)
    }

    @Test
    fun ipAddressUrlIsFlagged() {
        // http (15) + IP (30) + login (5) = 50
        val result = UrlRiskAnalyzer.analyze("http://192.168.1.10/login")

        assertTrue(result.score >= 45)
        assertTrue(result.reasons.any { it.contains("IP address") })
    }

    @Test
    fun longUrlAndHyphensIncreaseScore() {
        // https (0) + 3 hyphens (10) + >100 chars (10) = 20
        val longUrl = "https://this-is-a-very-long-url-with-many-hyphens-to-test-the-length-and-hyphen-detection-logic-of-the-new-model.com"
        val result = UrlRiskAnalyzer.analyze(longUrl)
        
        assertTrue(result.reasons.any { it.contains("long") })
        assertTrue(result.reasons.any { it.contains("hyphen") })
        assertTrue(result.score >= 20)
    }
}
