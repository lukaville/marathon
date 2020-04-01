package com.malinskiy.marathon

import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.Test

fun generateTestResult(): TestResult = generateTestResult(generateTest())

fun generateTestResult(
    test: Test,
    batchId: String = "test_batch_id"
): TestResult = TestResult(
    test,
    createDeviceInfo(),
    TestStatus.PASSED,
    0,
    10000,
    batchId
)

fun generateTestResults(tests: List<Test>): List<TestResult> {
    return tests.map {
        generateTestResult(it)
    }
}
