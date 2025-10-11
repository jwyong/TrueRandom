package com.truerandom.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TrackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("JAY_LOG", "TrackReceiver, onReceive: intent = $intent")
    }
}
