package com.example.scamguardv1.utils

object Logger {
    private const val TAG = "Verity"
    
    // Allows overriding for unit tests
    var printer: (String, String, Throwable?) -> Unit = { level, msg, t ->
        when (level) {
            "D" -> android.util.Log.d(TAG, msg)
            "E" -> android.util.Log.e(TAG, msg, t)
            "I" -> android.util.Log.i(TAG, msg)
        }
    }

    fun d(message: String) {
        try {
            printer("D", message, null)
        } catch (e: Exception) {
            println("D/$TAG: $message")
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        try {
            printer("E", message, throwable)
        } catch (e: Exception) {
            println("E/$TAG: $message ${throwable?.message}")
        }
    }

    fun i(message: String) {
        try {
            printer("I", message, null)
        } catch (e: Exception) {
            println("I/$TAG: $message")
        }
    }
}
