package com.malinskiy.marathon.report.logs

sealed class LogEvent {
    data class Crash(val message: String) : LogEvent()
}
