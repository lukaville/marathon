package com.malinskiy.marathon.report.summary

import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.test.Test

data class TestSummary(
    val test: Test,
    val results: List<TestResult>,
    val batches: List<Batch>
)
