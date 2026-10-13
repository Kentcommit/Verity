package com.example.scamguardv1.domain.model

data class UrlFeatures(
    val urlLength: Int,
    val hostLength: Int,
    val pathLength: Int,
    val digitCount: Int,
    val letterCount: Int,
    val specialCount: Int,
    val dotCount: Int,
    val hyphenCount: Int,
    val slashCount: Int,
    val atCount: Int,
    val percentCount: Int,
    val hasHttps: Boolean,
    val hasIpAddress: Boolean,
    val hasSuspiciousTerm: Boolean,
    val subdomainCount: Int,
    val queryParameterCount: Int,
    val uncommonExtension: Boolean,
    val hasPunycode: Boolean,
    val hasLoginPath: Boolean
)
