package com.info85.tuner85.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.info85.tuner85.data.model.RadioStation

class RadioPlayer(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    var playerListener: PlayerListener? = null

    interface PlayerListener {
        fun onPlaybackStateChanged(state: Int)
        fun onIsPlayingChanged(isPlaying: Boolean)
        fun onError(message: String)
    }

    fun initialize() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    playerListener?.onPlaybackStateChanged(playbackState)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playerListener?.onIsPlayingChanged(isPlaying)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playerListener?.onError(error.message ?: "Unknown error")
                }
            })
        }
    }

    fun playStation(station: RadioStation) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(station.streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
