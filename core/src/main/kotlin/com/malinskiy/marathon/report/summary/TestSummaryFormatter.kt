package com.malinskiy.marathon.report.summary

import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.test.toSimpleSafeTestName

class TestSummaryFormatter {

    fun formatTestResultSummary(currentTestResult: TestResult, testSummary: TestSummary?): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendln("Test status: ${currentTestResult.status}")
        stringBuilder.appendln("Strict run: ${currentTestResult.isStrictRun}")
        stringBuilder.appendln("From cache: ${currentTestResult.isFromCache}")

        testSummary?.let { summary ->
            stringBuilder.appendln()
            stringBuilder.appendln("There were ${summary.results.size} runs:")

            summary.results.forEach { testResult ->
                val isCurrent = testResult == currentTestResult
                val bulletSymbol = if (isCurrent) ">" else "*"

                stringBuilder.append("\u00a0\u00a0$bulletSymbol ${testResult.status}")

                val additionalInfo = listOfNotNull(
                    testResult.device.serialNumber,
                    "batch: " + testResult.batchId.createShortBatchId(),
                    testResult.createShortFailureDescription()
                )
                stringBuilder.append(" (${additionalInfo.joinToString()})")

                stringBuilder.appendln()
            }

            stringBuilder.appendln()

            stringBuilder.appendln("Test batches:")

            summary.batches.forEach { batch ->
                val isCurrentBatch = batch.batchId == currentTestResult.batchId
                val batchBulletSymbol = if (isCurrentBatch) ">" else "*"
                val deviceSerial = batch.testResults.first().device.serialNumber

                stringBuilder.appendln("\u00a0\u00a0$batchBulletSymbol ${batch.batchId.createShortBatchId()} (${batch.testResults.size} tests, $deviceSerial):")

                batch.testResults.forEach { testResult ->
                    val isCurrentTest = testResult.test == currentTestResult.test
                    val testBulletSymbol = if (isCurrentTest) ">" else "*"

                    stringBuilder.append("\u00a0\u00a0\u00a0\u00a0$testBulletSymbol ${testResult.test.toSimpleSafeTestName()}")

                    val additionalInfo = listOfNotNull(
                        testResult.status.toString(),
                        testResult.createShortFailureDescription()
                    )
                    stringBuilder.append(" (${additionalInfo.joinToString()})")
                    stringBuilder.appendln()
                }

                stringBuilder.appendln()
            }
        }

        return stringBuilder.toString()
    }

    private fun TestResult.createShortFailureDescription(): String? =
        stacktrace
            ?.lineSequence()
            ?.take(SHORT_FAILURE_MAX_LINES)
            ?.joinToString(separator = " ")
            ?.take(SHORT_FAILURE_DESCRIPTION_LIMIT)
            ?.let { "$it..." }

    private fun String.createShortBatchId(): String =
        take(SHORT_BATCH_ID_SIZE)

    private companion object {
        private const val SHORT_FAILURE_DESCRIPTION_LIMIT = 80
        private const val SHORT_FAILURE_MAX_LINES = 3
        private const val SHORT_BATCH_ID_SIZE = 8
    }
}
