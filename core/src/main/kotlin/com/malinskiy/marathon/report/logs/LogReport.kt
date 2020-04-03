package com.malinskiy.marathon.report.logs

import java.io.File

class LogReport(
    val batches: Map<String, BatchLogs>
)

class BatchLogs(
    val tests: Map<LogTest, Log>,
    val log: Log
)

class Log(
    val file: File,
    val events: List<LogEvent>
)

sealed class LogEvent {
    class Crash(val message: String) : LogEvent()
}
