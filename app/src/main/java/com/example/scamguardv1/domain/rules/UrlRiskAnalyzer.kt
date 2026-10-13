package com.example.scamguardv1.domain.rules

import com.example.scamguardv1.domain.model.DetectionResult
import com.example.scamguardv1.domain.model.UrlFeatures
import com.example.scamguardv1.utils.Logger
import java.net.URI
import java.util.Locale

/**
 * Improved URL analysis based on the Malicious URL Detection System (Extra Trees Model).
 * This logic extracts leakage-resistant features and calculates a risk score.
 */
object UrlRiskAnalyzer {
    private const val TAG = "UrlRiskAnalyzer"
    
    private val suspiciousExtensions = setOf(
        "tk", "ml", "ga", "cf", "gq", "top", "xyz", "click", "work", "zip"
    )
    
    private val suspiciousWords = listOf(
        "login", "signin", "verify", "verification", "account", "secure", "security",
        "update", "password", "bank", "banking", "wallet", "payment", "refund",
        "reward", "prize", "free", "offer", "urgent", "confirm", "otp", "kyc", "claim", "bonus"
    )

    fun analyze(rawValue: String): DetectionResult {
        val url = rawValue.trim()
        Logger.d("Analyzing URL: $url")
        if (url.isBlank()) {
            return DetectionResult(100f, listOf("The URL is empty."))
        }

        // Add protocol if missing for parsing
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        val uri = try {
            URI(formattedUrl)
        } catch (e: Exception) {
            Logger.e("URI constructor failed for $formattedUrl", e)
            null
        }

        val host = try {
            uri?.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: extractHostManually(formattedUrl)
        } catch (_: Exception) {
            extractHostManually(formattedUrl)
        }
        
        if (host.isBlank() && !hasIpAddress(url)) {
            Logger.i("Could not determine host for: $url")
            return DetectionResult(100f, listOf("Could not determine a valid host or IP address."))
        }

        val features = extractFeatures(url, uri, host)
        val (score, reasons) = calculateScore(features, url, host)

        val boundedScore = score.coerceIn(0, 100).toFloat()
        Logger.d("Analysis result for $host: score=$boundedScore, reasons=$reasons")

        return DetectionResult(
            score = boundedScore,
            reasons = reasons.ifEmpty { listOf("No suspicious patterns detected.") },
            urlFeatures = features
        )
    }

    private fun extractHostManually(url: String): String {
        return try {
            val withoutScheme = url.substringAfter("://")
            val authority = withoutScheme.substringBefore('/').substringBefore('?')
            authority.substringBefore(':').lowercase(Locale.ROOT).trimEnd('.')
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractFeatures(url: String, uri: URI?, host: String): UrlFeatures {
        val extension = host.substringAfterLast('.', "")
        
        return UrlFeatures(
            urlLength = url.length,
            hostLength = host.length,
            pathLength = (uri?.path?.length ?: 0) + (uri?.query?.length ?: 0) + 1,
            digitCount = url.count { it.isDigit() },
            letterCount = url.count { it.isLetter() },
            specialCount = url.count { it in "!@#$%^&*()_+-=[]{};:,.<>/?`~|" },
            dotCount = url.count { it == '.' },
            hyphenCount = host.count { it == '-' },
            slashCount = url.count { it == '/' },
            atCount = url.count { it == '@' },
            percentCount = url.count { it == '%' },
            hasHttps = url.startsWith("https", ignoreCase = true),
            hasIpAddress = hasIpAddress(host),
            hasSuspiciousTerm = suspiciousWords.any { url.contains(it, ignoreCase = true) },
            subdomainCount = maxOf(0, host.split('.').size - 2),
            queryParameterCount = uri?.query?.count { it == '=' } ?: 0,
            uncommonExtension = extension in suspiciousExtensions,
            hasPunycode = host.contains("xn--"),
            hasLoginPath = false
        )
    }

    private fun calculateScore(f: UrlFeatures, rawUrl: String, host: String): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()

        // 1. HTTPS Check (+15)
        if (!f.hasHttps) {
            score += 15
            reasons.add("Connection is not secured via HTTPS")
        }

        // 2. IP Address Check (+30)
        if (f.hasIpAddress) {
            score += 30
            reasons.add("Uses numeric IP address instead of domain name")
        }

        // 3. @ Symbol Check (+20)
        if (rawUrl.contains("@")) {
            score += 20
            reasons.add("Contains '@' symbol (often used to disguise destinations)")
        }

        // 4. Suspicious Words (+5 each, max 25)
        var wordCount = 0
        val foundWords = mutableListOf<String>()
        suspiciousWords.forEach { word ->
            if (rawUrl.contains(word, ignoreCase = true)) {
                wordCount++
                if (foundWords.size < 3) foundWords.add(word)
            }
        }
        if (wordCount > 0) {
            val penalty = minOf(wordCount * 5, 25)
            score += penalty
            reasons.add("Contains suspicious terms: ${foundWords.joinToString(", ")}${if (wordCount > 3) "..." else ""}")
        }

        // 5. Domain Analysis
        if (f.uncommonExtension) {
            score += 15
            reasons.add("Uses a high-risk domain extension (.${host.substringAfterLast('.')})")
        }

        if (f.subdomainCount >= 3) {
            score += 10
            reasons.add("Excessive number of subdomains")
        }

        // 6. Many Hyphens in Hostname (+10)
        if (f.hyphenCount >= 3) {
            score += 10
            reasons.add("Hostname contains multiple hyphens")
        }

        // 7. Long URL (+10 for >100, +10 for >200)
        if (f.urlLength > 100) {
            score += 10
            reasons.add("URL is unusually long (>100 chars)")
        }
        if (f.urlLength > 200) {
            score += 10
        }

        // 8. Encoded Characters (+10)
        if (f.percentCount >= 5) {
            score += 10
            reasons.add("Contains many encoded characters (%)")
        }

        if (f.hasPunycode) {
            score += 30
            reasons.add("Potential look-alike character spoofing (Punycode)")
        }

        return score to reasons
    }


    private fun hasIpAddress(host: String): Boolean {
        val ipv4Regex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipv4Regex.matches(host)) {
            return host.split('.').all { it.toIntOrNull() in 0..255 }
        }
        if (host.contains(':') && host.count { it == ':' } >= 2) return true
        return false
    }
}

