package com.info85.tuner85.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.info85.tuner85.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RadioService : Service() {

    private val binder = RadioBinder()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: MediaNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentStation: RadioStation? = null
        private set

    var stationList: List<RadioStation> = emptyList()
    var currentIndex: Int = 0

    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onStationChanged: ((RadioStation) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    inner class RadioBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    override fun onCreate() {
        super.onCreate()
        initPlayer()
        notificationManager = MediaNotificationManager(this)
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlaybackStateChanged?.invoke(isPlaying)
                    updateNotification()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    onError?.invoke(error.message ?: "Playback error")
                }
            })
        }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MediaNotificationManager.ACTION_PLAY -> play()
            MediaNotificationManager.ACTION_PAUSE -> pause()
            MediaNotificationManager.ACTION_NEXT -> nextStation()
            MediaNotificationManager.ACTION_PREVIOUS -> previousStation()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun playStation(station: RadioStation) {
        currentStation = station
        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtworkUri(android.net.Uri.parse(station.logoUrl))
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        onStationChanged?.invoke(station)
        updateNotification()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun nextStation() {
        if (stationList.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % stationList.size
            playStation(stationList[currentIndex])
        }
    }

    fun previousStation() {
        if (stationList.isNotEmpty()) {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else stationList.size - 1
            playStation(stationList[currentIndex])
        }
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun getPlayer(): ExoPlayer = player

    fun getMediaSession(): MediaSession = mediaSession

    private fun updateNotification() {
        val station = currentStation
        if (station != null) {
            serviceScope.launch {
                val bitmap = loadBitmap(station.logoUrl)
                val notification = notificationManager.buildNotification(
                    station, player.isPlaying, mediaSession, bitmap
                )
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
            }
        } else {
            val notification = notificationManager.buildNotification(
                null, player.isPlaying, mediaSession, null
            )
            startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        if (url.isEmpty()) return null
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
