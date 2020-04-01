package com.malinskiy.marathon.android.executor.logcat.parse

import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent

interface LogcatEventsListener {
    fun onLogcatEvent(event: LogcatEvent)
}
