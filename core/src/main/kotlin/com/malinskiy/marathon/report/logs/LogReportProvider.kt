package com.malinskiy.marathon.report.logs

interface LogReportProvider {
    fun getLogReport(): LogReport
}
