package com.example.scamguardv1.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.example.scamguardv1.domain.rules.RiskAnalysis
import com.example.scamguardv1.utils.Logger
import kotlinx.coroutines.*

class CallOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var contentView: TextView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentPhoneNumber: String = "Unknown"
    private var currentScore: Float = 0f
    private var currentReasons: List<String> = emptyList()
    private var isScanningStarted = false

    private lateinit var audioManager: AudioManager
    private lateinit var telephonyManager: TelephonyManager

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                handleStateChange(state)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing analysis..."))
        
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentPhoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        currentScore = intent?.getFloatExtra(EXTRA_SCORE, 0f) ?: 0f
        currentReasons = intent?.getStringArrayExtra(ScamCallScreeningService.EXTRA_REASONS)?.toList() ?: emptyList()

        showOverlay(currentPhoneNumber)
        
        // Start a polling observer for VoIP calls (which may not trigger Telephony broadcasts)
        startCallStateObserver()
        
        return START_NOT_STICKY
    }

    private fun handleStateChange(state: String?) {
        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (!isScanningStarted) {
                    isScanningStarted = true
                    startScanning()
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                removeOverlay()
                stopSelf()
            }
        }
    }

    private fun startCallStateObserver() {
        serviceScope.launch {
            while (isActive) {
                // Check if call is answered (VoIP often uses MODE_IN_COMMUNICATION)
                val isVoipActive = audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
                val isPstnActive = telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK

                if ((isVoipActive || isPstnActive) && !isScanningStarted) {
                    isScanningStarted = true
                    startScanning()
                    // Don't break yet, we might need it to detect end of call for VoIP
                }

                // Detect end of call for VoIP (since it doesn't trigger EXTRA_STATE_IDLE)
                if (isScanningStarted && !isVoipActive && !isPstnActive) {
                    // Check for a brief period to ensure it's not a temporary mode switch
                    delay(2000)
                    if (audioManager.mode == AudioManager.MODE_NORMAL && telephonyManager.callState == TelephonyManager.CALL_STATE_IDLE) {
                        removeOverlay()
                        stopSelf()
                        break
                    }
                }

                delay(1000) // Poll every second
            }
        }
    }

    private fun showOverlay(phoneNumber: String) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        overlayView = FrameLayout(this).apply {
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            setPadding(32, 32, 32, 32)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        contentView = TextView(this).apply {
            text = "Call incoming from $phoneNumber\nWaiting for answer..."
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
        }
        (overlayView as FrameLayout).addView(contentView)

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Logger.e("Could not add overlay view", e)
            stopSelf()
            return
        }
    }

    private fun startScanning() {
        serviceScope.launch {
            contentView?.text = "Call answered. Scanning for threats..."
            
            // Scan for 5 seconds
            val scanDuration = 5000L
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < scanDuration) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed * 100 / scanDuration).toInt()
                contentView?.text = "Scanning voice patterns... $progress%"
                delay(100)
            }
            
            contentView?.text = "Analysis complete. Scoring..."
            delay(500)
            
            val risk = RiskAnalysis.getRiskLevel(currentScore)
            contentView?.text = "Voice Manipulation Risk: ${currentScore.toInt()}%\n${risk.label}"
            contentView?.setTextColor(risk.color.toArgb())

            // Now "score the threat" by notifying the rest of the app
            val intent = Intent(ScamCallScreeningService.ACTION_CALL_SCREENED).apply {
                putExtra(ScamCallScreeningService.EXTRA_PHONE_NUMBER, currentPhoneNumber)
                putExtra(ScamCallScreeningService.EXTRA_SCORE, currentScore)
                putExtra(ScamCallScreeningService.EXTRA_REASONS, currentReasons.toTypedArray())
            }
            sendBroadcast(intent)
            
            // Keep the results visible for a while before letting the observer clean up
            delay(10000)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed or not added
            }
            overlayView = null
            contentView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Analysis Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Verity Live Analysis")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        removeOverlay()
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_SCORE = "extra_score"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_analysis_channel"
    }
}
