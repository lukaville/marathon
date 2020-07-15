package com.malinskiy.marathon.android.executor.listeners.video

import java.util.concurrent.CopyOnWriteArrayList

class ScreenRecorderHandler {

    private val listeners: MutableList<() -> Unit> = CopyOnWriteArrayList()

    @Volatile
    private var stopped: Boolean = false

    fun stop() {
        stopped = true
        listeners.forEach { it() }
    }

    fun subscribeOnStop(onStop: () -> Unit) {
        if (stopped) {
            onStop()
        } else {
            listeners += onStop
        }
    }
}
