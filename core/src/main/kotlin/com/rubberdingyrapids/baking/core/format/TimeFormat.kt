package com.rubberdingyrapids.baking.core.format

object TimeFormat {
    /** "1h 20m", "45m", "30s", "1h 20m 05s" is avoided for readability. */
    fun short(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return buildString {
            if (h > 0) append("${h}h")
            if (m > 0) { if (isNotEmpty()) append(' '); append("${m}m") }
            if (h == 0 && sec > 0) { if (isNotEmpty()) append(' '); append("${sec}s") }
            if (isEmpty()) append("0s")
        }
    }

    /** "01:20:05" or "20:05" for a running countdown. */
    fun clock(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
