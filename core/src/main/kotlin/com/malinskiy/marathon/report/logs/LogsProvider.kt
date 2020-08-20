package com.malinskiy.marathon.report.logs

interface LogsProvider {
    fun getFullReport(): LogReport
    suspend fun getBatchReport(batchId: String): BatchLogs?
}
