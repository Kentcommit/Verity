package com.example.scamguardv1.utils

object Constants {
    // Scoring Thresholds
    const val HIGH_RISK_THRESHOLD = 70f
    const val MEDIUM_RISK_THRESHOLD = 40f
    
    // Feature Suspicion Weights
    const val WEIGHT_ENERGY_STABILITY_HIGH = 40f
    const val WEIGHT_ENERGY_STABILITY_MEDIUM = 20f
    const val WEIGHT_SPECTRAL_CENTROID = 30f
    const val WEIGHT_ZCR_ANOMALY = 20f
    const val WEIGHT_CALL_VERIFICATION_FAILED = 50f
    
    // Feature Bounds
    const val STABILITY_THRESHOLD_HIGH = 0.85f
    const val STABILITY_THRESHOLD_MEDIUM = 0.7f
    const val CENTROID_BOUND_LOW = 0.4f
    const val CENTROID_BOUND_HIGH = 0.6f
    const val ZCR_BOUND_LOW = 0.05f
    const val ZCR_BOUND_HIGH = 0.4f
    
    // UI Constants
    const val SCAN_PROGRESS_ANIMATION_DURATION = 300
    const val AUDIO_PICKER_TYPE = "audio/*"
    
    // Error Messages
    const val ERROR_READ_FAILED = "Could not read audio data"
    const val ERROR_UNKNOWN = "An unexpected error occurred"
    const val ERROR_LOAD_FILES_FAILED = "Failed to load audio files"

    // Preferences
    const val PREFS_NAME = "scamguard_prefs"
    const val PREF_CALL_SCREENING_ENABLED = "pref_call_screening_enabled"
    const val PREF_DELETED_AUDIO_FILES = "pref_deleted_audio_files"
    const val PREF_AUTO_DELETE_DAYS = "pref_auto_delete_days"

    // Persisted Data Keys
    const val PREF_LATEST_RISK_SCORE = "latest_risk_score"
    const val PREF_APP_THEME = "app_theme"
    const val PREF_SCREENED_CALLS = "persisted_screened_calls"
    const val PREF_SCAN_HISTORY = "persisted_scan_history"
    const val PREF_PROFILE_PIC_URI = "profile_pic_uri"
    const val PREF_DISPLAY_NAME = "display_name"
}
