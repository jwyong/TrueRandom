package com.truerandom.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.truerandom.util.NotificationUtil


class TrackService: Service() {
    override fun onCreate() {
        super.onCreate()

        Log.d("JAY_LOG", "TrackService, onCreate: ")

        // Show foreground notification
        NotificationUtil.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("JAY_LOG", "TrackService, onStartCommand: ")

        // Start foreground service as normal first
        startForeground(
            NotificationUtil.NOTIFICATION_ID, NotificationUtil.createNotification(this)
        )

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        lateinit var codeVerifier: String  // Store this somewhere safe temporarily

        // Whether is currently playing
        var isPlaying = false

        // Currently playing track uri
        var currentTrackUri: String? = null
    }
}