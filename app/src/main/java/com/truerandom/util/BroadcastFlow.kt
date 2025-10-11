package com.truerandom.util

import android.content.BroadcastReceiver
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * [MutableSharedFlow] which acts similar to a [BroadcastReceiver] to receive inter-class "broadcasts".
 **/
class BroadcastFlow<T> {
    private val internalFlow = MutableSharedFlow<T>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val flow: SharedFlow<T> = internalFlow

    fun tryEmit(value: T): Boolean = internalFlow.tryEmit(value)

    suspend fun emit(value: T) = internalFlow.emit(value)
}
