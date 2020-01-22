package com.malinskiy.marathon.android.executor.listeners.video

class ScreenRecorderHandler {

    private val listeners: MutableList<() -> Unit> = arrayListOf()

    fun stop() {
        listeners.forEach { it() }
    }

    fun subscribeOnStop(onStop: () -> Unit) {
        listeners += onStop
    }
}
