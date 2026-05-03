package com.info85.tuner85.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.info85.tuner85.MainActivity
import com.info85.tuner85.R
import com.info85.tuner85.data.model.RadioStation

class MediaNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "radio_tuner_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.info85.tuner85.ACTION_PLAY"
        const val ACTION_PAUSE = "com.info85.tuner85.ACTION_PAUSE"
        const val ACTION_NEXT = "com.info85.tuner85.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.info85.tuner85.ACTION_PREVIOUS"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Radio Tuner 85",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Radio playback controls"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        station: RadioStation?,
        isPlaying: Boolean,
        mediaSession: MediaSession,
        albumArt: Bitmap? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                context.getString(R.string.pause),
                createActionIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                context.getString(R.string.play),
                createActionIntent(ACTION_PLAY)
            )
        }

        val prevAction = NotificationCompat.Action(
            R.drawable.ic_previous,
            context.getString(R.string.previous),
            createActionIntent(ACTION_PREVIOUS)
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.ic_next,
            context.getString(R.string.next),
            createActionIntent(ACTION_NEXT)
        )

        val largeIcon = albumArt ?: vectorToBitmap(R.drawable.ic_radio)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(station?.name ?: context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.streaming))
            .setSmallIcon(R.drawable.ic_radio)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession).setShowActionsInCompactView(0, 1, 2))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .build()
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(context, RadioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun vectorToBitmap(drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
