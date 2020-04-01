package com.malinskiy.marathon.android.executor.logcat.model

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.report.logs.LogTest

sealed class LogcatEvent(val device: AndroidDevice) {

    class Message(
        val logcatMessage: LogcatMessage,
        device: AndroidDevice
    ) : LogcatEvent(device)

    class BatchStarted(
        val batchId: String,
        device: AndroidDevice
    ) : LogcatEvent(device)

    class BatchFinished(
        val batchId: String,
        device: AndroidDevice
    ) : LogcatEvent(device)

    class TestStarted(
        val test: LogTest,
        val processId: Int,
        device: AndroidDevice
    ) : LogcatEvent(device)

    class TestFinished(
        val test: LogTest,
        val processId: Int,
        device: AndroidDevice
    ) : LogcatEvent(device)

    class DeviceDisconnected(device: AndroidDevice) : LogcatEvent(device)

}
