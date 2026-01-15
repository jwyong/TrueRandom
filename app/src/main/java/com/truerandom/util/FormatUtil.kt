package com.truerandom.util

object FormatUtil {
    /**
     * Converts milliseconds to a formatted string (mm:ss).
     * Example: 299000ms -> "04:59"
     */
    fun Long?.toFormattedTime(): String {
        if (this == null) return "00:00"

        val totalSeconds = this / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        // Manual string building is much faster than String.format()
        val m = if (minutes < 10) "0$minutes" else "$minutes"
        val s = if (seconds < 10) "0$seconds" else "$seconds"

        return "$m:$s"
    }
}