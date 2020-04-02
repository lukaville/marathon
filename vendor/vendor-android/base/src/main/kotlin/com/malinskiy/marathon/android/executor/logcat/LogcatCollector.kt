package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.executor.logcat.BatchLogSaver.SaveEntry
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent
import com.malinskiy.marathon.android.executor.logcat.parse.LogcatEventsListener
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.report.logs.LogTest
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.LogReport
import com.malinskiy.marathon.report.logs.LogReportProvider

class LogcatCollector : LogcatEventsListener, LogReportProvider {

    private val logger = MarathonLogging.logger(LogcatCollector::class.java.simpleName)

    private val devices: MutableMap<Device, DeviceState> = hashMapOf()
    private val batchCollectors: MutableMap<String, BatchLogSaver> = hashMapOf()

    @Synchronized
    override fun onLogcatEvent(event: LogcatEvent) {
        when (event) {
            is LogcatEvent.Message -> {
                val currentBatchId: String = devices[event.device]?.currentBatchId ?: return
                val currentTest = devices[event.device]?.currentTest
                val batchCollector = batchCollectors.getOrPut(currentBatchId) { BatchLogSaver() }
                val entry = SaveEntry.Message(event.logcatMessage)
                batchCollector.save(entry, currentTest)
            }
            is LogcatEvent.BatchStarted -> {
                devices.compute(event.device) { _, oldState ->
                    val state = oldState ?: DeviceState()
                    state.copy(currentBatchId = event.batchId, currentTest = null)
                }
            }
            is LogcatEvent.BatchFinished -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: batch ${event.batchId} finished but not started (state = ${oldState})" }
                    return
                }
                batchCollectors[event.batchId]?.close()
                devices[event.device] = oldState.copy(currentBatchId = null, currentTest = null)
            }
            is LogcatEvent.TestStarted -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: test ${event.test} started but no active batches found (state = ${oldState})" }
                    return
                }

                devices[event.device] = oldState.copy(currentTest = event.test)
            }
            is LogcatEvent.TestFinished -> {
                val oldState = devices[event.device]
                if (oldState?.currentBatchId == null) {
                    logger.error { "Incorrect state: test ${event.test} finished but no active batches found (state = ${oldState})" }
                    return
                }

                if (oldState.currentTest != event.test) {
                    logger.error { "Incorrect state: test ${event.test} finished but current active test is ${oldState.currentTest}" }
                    return
                }

                batchCollectors[oldState.currentBatchId]?.close(event.test)
                devices[event.device] = oldState.copy(currentTest = null)
            }
            is LogcatEvent.DeviceDisconnected -> {
                val currentBatchId = devices[event.device]?.currentBatchId
                batchCollectors[currentBatchId]?.close()
                devices[event.device] = DeviceState()
            }
        }
    }

    @Synchronized
    override fun getLogReport(): LogReport =
        LogReport(batchCollectors.mapValues { it.value.createBatchLogs() })

    private data class DeviceState(
        val currentBatchId: String? = null,
        val currentTest: LogTest? = null
    )
}
