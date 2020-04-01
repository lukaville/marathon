package com.malinskiy.marathon.report.logs

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEventInflator
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.test.Test

class LogReportTestEventInflator(private val logReport: LogReport) : TestEventInflator {

    override fun inflate(event: TestEvent): TestEvent {
        // TODO: add more info to "TestResult.stacktrace" based on crash events from LogReport

        val additionalAttachments = listOfNotNull(createLogAttachment(event.testResult))

        val testResult = event.testResult.copy(
            attachments = event.testResult.attachments + additionalAttachments
        )

        return event.copy(testResult = testResult)
    }

    private fun createLogAttachment(testResult: TestResult): Attachment? {
        val batchId = testResult.batchId
        val logTest = testResult.test.toLogTest()
        val batchLogs = logReport.batches[batchId] ?: return null
        val log = batchLogs.tests.getOrDefault(logTest, batchLogs.log)
        return Attachment(log.file, AttachmentType.LOG, FileType.LOG)
    }

    private fun Test.toLogTest(): LogTest =
        LogTest(pkg, clazz, method)
}
