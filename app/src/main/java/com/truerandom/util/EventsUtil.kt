package com.truerandom.util

object EventsUtil {
    // When a specific track is tapped
    val playTrackEventFlow = BroadcastFlow<String>()
    fun sendPlayTrackEvent(trackUri: String) {
        playTrackEventFlow.tryEmit(trackUri)
    }

    // When buttons are tapped
    val playPauseButtonEventFlow = BroadcastFlow<Unit>()
    fun sendPlayPauseButtonEvent() {
        playPauseButtonEventFlow.tryEmit(Unit)
    }

    val prevNextButtonEventFlow = BroadcastFlow<Boolean>()
    fun sendPrevNextButtonEvent(isNext: Boolean) {
        prevNextButtonEventFlow.tryEmit(isNext)
    }

    // When a track playback has ended
    val trackPlaybackEndEventFlow = BroadcastFlow<Unit>()
    fun sendTrackPlaybackEndEvent() {
        trackPlaybackEndEventFlow.tryEmit(Unit)
    }

    // Register the system media callback
    val registerMediaCallbackEventFlow = BroadcastFlow<Boolean>()
    fun sendRegisterMediaCallbackEvent(isStart: Boolean) {
        registerMediaCallbackEventFlow.tryEmit(isStart)
    }
}