package com.truerandom.util

object EventsUtil {
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
}