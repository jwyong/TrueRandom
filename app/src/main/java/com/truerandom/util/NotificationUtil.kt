package com.truerandom.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.truerandom.R
import com.truerandom.receiver.TrackReceiver
import com.truerandom.service.TrackService

object NotificationUtil {
    const val NOTIFICATION_ID = 18473
    private const val CHANNEL_ID = "true_foreground"

    private const val PREV_TRACK_REQUEST_CODE = 1523
    private const val PLAY_PAUSE_TRACK_REQUEST_CODE = 5123
    private const val NEXT_TRACK_REQUEST_CODE = 3467

    fun createNotificationChannel(context: Context) {
        // Create the notification channel for Android 8.0+
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.foreground_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun createNotification(context: Context): Notification {
        val prevTrackIntent = Intent(context, TrackReceiver::class.java).apply {
            action = ACTION_PREV_TRACK
        }
        val prevTrackPendingIntent = PendingIntent.getBroadcast(
            context,
            PREV_TRACK_REQUEST_CODE,
            prevTrackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playTrackIntent = Intent(context, TrackReceiver::class.java).apply {
            action = if (TrackService.isPlaying) ACTION_PAUSE_TRACK else ACTION_PLAY_TRACK
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            PLAY_PAUSE_TRACK_REQUEST_CODE,
            playTrackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTrackIntent = Intent(context, TrackReceiver::class.java).apply {
            action = ACTION_NEXT_TRACK
        }
        val nextTrackPendingIntent = PendingIntent.getBroadcast(
            context,
            NEXT_TRACK_REQUEST_CODE,
            nextTrackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set title and btn text based on toggle state
        val playPauseText = if (TrackService.isPlaying) {
            R.string.notification_pause
        } else {
            R.string.notification_play
        }.let {
            context.getString(it)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.foreground_desc))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .addAction(
                PREV_TRACK_REQUEST_CODE,
                context.getString(R.string.notification_prev),
                prevTrackPendingIntent
            )
            .addAction(PLAY_PAUSE_TRACK_REQUEST_CODE, playPauseText, playPausePendingIntent)
            .addAction(
                NEXT_TRACK_REQUEST_CODE,
                context.getString(R.string.notification_next),
                nextTrackPendingIntent
            )
            .setOngoing(true)
            .build()
    }
}
