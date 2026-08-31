package com.example.scamguardv1.domain.player

import android.net.Uri

interface VoicePlayer {
    fun prepare(uri: Uri, onComplete: () -> Unit = {})
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun seekTo(positionMs: Long)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
}
