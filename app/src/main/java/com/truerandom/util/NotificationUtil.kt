package com.truerandom.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.truerandom.R
import com.truerandom.receiver.NotificationReceiver
import com.truerandom.service.TrackService
import com.truerandom.ui.MainActivity

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
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.foreground_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun createNotification(context: Context): Notification {
        val prevTrackIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_PREV_TRACK
        }
        val prevTrackPendingIntent = PendingIntent.getBroadcast(
            context,
            PREV_TRACK_REQUEST_CODE,
            prevTrackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playTrackIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = if (TrackService.isPlaying) ACTION_PAUSE_TRACK else ACTION_PLAY_TRACK
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            PLAY_PAUSE_TRACK_REQUEST_CODE,
            playTrackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTrackIntent = Intent(context, NotificationReceiver::class.java).apply {
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

        // Launch app when tap on notification body
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            // Optional: Add flags to ensure a clean launch
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0, // Request code, 0 is common for the main content intent
            launchIntent,
            // Use FLAG_IMMUTABLE (required on Android 12+) or FLAG_UPDATE_CURRENT
            PendingIntent.FLAG_IMMUTABLE
        )

        // Get content string (<trackName> - <trackArtists>)
        val trackLabel = if (TrackService.currentTrackLabel.isBlank()) {
            context.getString(R.string.unknown)
        } else {
            TrackService.currentTrackLabel
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(trackLabel)
            .setContentIntent(contentPendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
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

        return builder.build()
    }
}
