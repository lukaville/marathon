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

                stringBuilder.append("\t$bulletSymbol ${currentTestResult.status}")

                val additionalInfo = listOfNotNull(
                    testResult.device.serialNumber,
                    "batch: " + testResult.batchId,
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

                stringBuilder.appendln("\t$batchBulletSymbol ${batch.batchId} (${batch.testResults.size} tests, $deviceSerial):")

                batch.testResults.forEach { testResult ->
                    val isCurrentTest = testResult == currentTestResult
                    val testBulletSymbol = if (isCurrentTest) ">" else "*"

                    stringBuilder.append("\t\t$testBulletSymbol ${testResult.test.toSimpleSafeTestName()}")

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
            ?.take(SHORT_FAILURE_DESCRIPTION_LIMIT) + "..."

    private companion object {
        private const val SHORT_FAILURE_DESCRIPTION_LIMIT = 80
        private const val SHORT_FAILURE_MAX_LINES = 80
    }
}
