package com.truerandom.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.truerandom.util.ACTION_NEXT_TRACK
import com.truerandom.util.ACTION_PAUSE_TRACK
import com.truerandom.util.ACTION_PLAY_TRACK
import com.truerandom.util.ACTION_PREV_TRACK
import com.truerandom.util.EventsUtil

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("JAY_LOG", "TrackReceiver, onReceive: intent = $intent")

        when (intent.action) {
            ACTION_PLAY_TRACK, ACTION_PAUSE_TRACK -> EventsUtil.sendPlayPauseButtonEvent()
            ACTION_PREV_TRACK -> EventsUtil.sendPrevNextButtonEvent(false)
            ACTION_NEXT_TRACK -> EventsUtil.sendPrevNextButtonEvent(true)
        }
    }
}
