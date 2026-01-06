package com.truerandom.util

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.truerandom.ui.TAG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LogUtil {
    private val logHistory = StringBuilder()

    // This holds the actual data
    private val _logs = MutableLiveData<String>()

    // This is what the UI observes (read-only)
    val logs: LiveData<String> = _logs

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }

    fun d(tag: String, msg: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "$timestamp D/$msg\n"

        Log.d(TAG, "LogUtil, d: $msg")
        synchronized(logHistory) {
            logHistory.append(logLine)
            _logs.postValue(logHistory.toString())
        }
    }

    // Clear logs if needed
    fun clearLogs() {
        synchronized(logHistory) {
            logHistory.clear()
            _logs.postValue(logHistory.toString())
        }
    }
}