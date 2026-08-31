package com.example.scamguardv1.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.scamguardv1.domain.player.VoicePlayer

class ExoVoicePlayer(context: Context) : VoicePlayer {
    private val player = ExoPlayer.Builder(context).build()
    private var onCompleteCallback: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompleteCallback?.invoke()
                }
            }
        })
    }

    override fun prepare(uri: Uri, onComplete: () -> Unit) {
        onCompleteCallback = onComplete
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.release()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun getCurrentPosition(): Long = player.currentPosition

    override fun getDuration(): Long = player.duration

    override fun isPlaying(): Boolean = player.isPlaying
}
