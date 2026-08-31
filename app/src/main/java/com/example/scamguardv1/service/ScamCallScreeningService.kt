package com.example.scamguardv1.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.content.Intent
import android.os.Build
import com.example.scamguardv1.data.ScamDetector
import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.utils.Constants
import com.example.scamguardv1.utils.Logger

class ScamCallScreeningService : CallScreeningService() {

    private lateinit var scamDetector: ScamDetector

    override fun onCreate() {
        super.onCreate()
        scamDetector = ScamDetector(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            // On older versions, we assume it's incoming if we are screening
            true 
        }
        
        if (isIncoming) {
            val handle = callDetails.handle?.schemeSpecificPart ?: "Unknown"
            Logger.i("Screening incoming call from: $handle")

            // Base score from metadata
            var metadataScore = 0f
            val reasons = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED) {
                    metadataScore += Constants.WEIGHT_CALL_VERIFICATION_FAILED
                    reasons.add("Network verification failed (STIR/SHAKEN)")
                }
            }

            // Since we can't get real-time audio stream easily in CallScreeningService 
            // on standard Android without being a system dialer, we simulate/heuristic 
            // the voice analysis part if metadata is suspicious or just to show the logic.
            
            // Heuristic: If number is not verified, we "detect" suspicious voice patterns
            val finalResult = if (metadataScore > 0) {
                val mockFeatures = VoiceFeatures(
                    spectralCentroid = 0.5f, // Within suspicious bound
                    zeroCrossingRate = 0.04f, // Below bound
                    energyStability = 0.9f,   // High stability (robotic)
                    harmonicsRatio = 0.8f,
                )
                scamDetector.analyzeVoice(mockFeatures)
            } else {
                scamDetector.analyzeVoice(VoiceFeatures(0.1f, 0.2f, 0.1f, 0.2f))
            }

            val totalScore = (metadataScore + finalResult.score).coerceIn(0f, 100f)
            val combinedReasons = reasons + finalResult.reasons

            Logger.d("Call screening result for $handle: Score $totalScore")

            // Start Overlay Service
            val overlayIntent = Intent(this, CallOverlayService::class.java).apply {
                putExtra(CallOverlayService.EXTRA_PHONE_NUMBER, handle)
                putExtra(CallOverlayService.EXTRA_SCORE, totalScore)
                putExtra(EXTRA_REASONS, combinedReasons.toTypedArray())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }

            val response = CallResponse.Builder().apply {
                if (totalScore >= Constants.HIGH_RISK_THRESHOLD) {
                    setDisallowCall(true)
                    setRejectCall(true)
                    setSkipCallLog(false)
                    setSkipNotification(false)
                }
            }.build()

            respondToCall(callDetails, response)
        }
    }

    companion object {
        const val ACTION_CALL_SCREENED = "com.example.scamguardv1.CALL_SCREENED"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_REASONS = "extra_reasons"
    }
}
