package com.malinskiy.marathon.report.summary

import com.google.gson.Gson
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.report.summary.json.BatchInfo
import com.malinskiy.marathon.report.summary.json.TestInfo
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName

class TestSummaryFormatter {

    private val gson: Gson by lazy { Gson() }

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
                val batchSummary = batch.toSummaryString(currentTestResult.batchId, currentTestResult.test)
                stringBuilder.append(batchSummary)
                stringBuilder.appendln()
            }
        }

        return stringBuilder.toString()
    }

    private fun Batch.toSummaryString(currentBatchId: String, currentTest: Test): String {
        val stringBuilder = StringBuilder()
        val isCurrentBatch = batchId == currentBatchId
        val batchBulletSymbol = if (isCurrentBatch) ">" else "*"
        val deviceSerial = testResults.first().device.serialNumber

        val testStatus = testResults
            .firstOrNull { it.test == currentTest }
            ?.status
            ?.toString() ?: "UNKNOWN"

        stringBuilder.appendln("\u00a0\u00a0$batchBulletSymbol $testStatus #${batchId.createShortBatchId()} (${testResults.size} tests, $deviceSerial):")

        testResults.forEach { testResult ->
            val isCurrentTest = testResult.test == currentTest
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
        stringBuilder.appendln("\u00a0\u00a0\u00a0\u00a0Copy this JSON to run it locally:")
        stringBuilder.append("\u00a0\u00a0\u00a0\u00a0")
        stringBuilder.appendln(toJsonString())
        stringBuilder.appendln()

        return stringBuilder.toString()
    }

    private fun Batch.toJsonString(): String {
        val component = this.testResults.first().test.componentInfo.gradleModulePath
        val jsonTests = testResults.map { TestInfo(it.test.pkg, it.test.clazz, it.test.method) }
        val jsonBatch = BatchInfo(this.batchId, component, jsonTests)
        return gson.toJson(jsonBatch)
    }

    private val ComponentInfo.gradleModulePath: String
        get() = name.substringBeforeLast(":")

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
