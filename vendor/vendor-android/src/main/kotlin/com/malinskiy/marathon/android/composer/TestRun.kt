package com.malinskiy.marathon.android.composer

import com.gojuno.commander.android.adb
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.nanosToHumanReadableTime
import com.gojuno.commander.os.process
import com.malinskiy.marathon.android.AndroidDevice
import rx.Observable
import rx.Single
import java.io.File
import java.nio.file.Files

data class AdbDeviceTestRun(
    val adbDevice: AndroidDevice,
    val tests: List<AdbDeviceTest>,
    val passedCount: Int,
    val ignoredCount: Int,
    val failedCount: Int,
    val durationNanos: Long,
    val timestampMillis: Long,
    val logcat: File,
    val instrumentationOutput: File
)

data class AdbDeviceTest(
    val adbDevice: AndroidDevice,
    val className: String,
    val testName: String,
    val status: Status,
    val durationNanos: Long,
    val logcat: File,
    val files: List<File>,
    val screenshots: List<File>
) {
    sealed class Status {
        object Passed : Status()
        data class Ignored(val stacktrace: String) : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}

fun AndroidDevice.runTests(
    testPackageName: String,
    testRunnerClass: String,
    instrumentationArguments: String,
    keepOutput: Boolean
): Single<AdbDeviceTestRun> {

    val adbDevice = this
    val logsDir =  Files.createTempDirectory("adb_logs").toFile().apply {
        mkdirs()
        deleteOnExit()
    }
    val instrumentationOutputFile = File(logsDir, "instrumentation.output")

    val runTests = process(
        commandAndArgs = listOf(
            adb,
            "-s", adbDevice.ddmsDevice.serialNumber,
            "shell", "am instrument -w -r $instrumentationArguments $testPackageName/$testRunnerClass"
        ),
        timeout = null,
        redirectOutputTo = instrumentationOutputFile,
        keepOutputOnExit = keepOutput
    ).share()

    @Suppress("destructure")
    val runningTests = runTests
        .ofType(Notification.Start::class.java)
        .flatMap { readInstrumentationOutput(it.output) }
        .asTests()
        .doOnNext { test ->
            val status = when (test.status) {
                is InstrumentationTest.Status.Passed -> "passed"
                is InstrumentationTest.Status.Ignored -> "ignored"
                is InstrumentationTest.Status.Failed -> "failed"
            }

            println(
                "Test ${test.index}/${test.total} $status in " +
                        "${test.durationNanos.nanosToHumanReadableTime()}: " +
                        "${test.className}.${test.testName}"
            )
        }
        .toList()

    val adbDeviceTestRun = Observable
        .zip(
            Observable.fromCallable { System.nanoTime() },
            runningTests,
            { time, tests -> time to tests }
        )
        .map { (startTimeNanos, testsWithPulledFiles) ->

            AdbDeviceTestRun(
                adbDevice = adbDevice,
                tests = testsWithPulledFiles.map { test ->
                    AdbDeviceTest(
                        adbDevice = adbDevice,
                        className = test.className,
                        testName = test.testName,
                        status = when (test.status) {
                            is InstrumentationTest.Status.Passed -> AdbDeviceTest.Status.Passed
                            is InstrumentationTest.Status.Ignored -> AdbDeviceTest.Status.Ignored(test.status.stacktrace)
                            is InstrumentationTest.Status.Failed -> AdbDeviceTest.Status.Failed(test.status.stacktrace)
                        },
                        durationNanos = test.durationNanos,
                        logcat = logcatFileForTest(logsDir, test.className, test.testName),
                        files = emptyList(),
                        screenshots = emptyList()
                    )
                },
                passedCount = testsWithPulledFiles.count { it.status is InstrumentationTest.Status.Passed },
                ignoredCount = testsWithPulledFiles.count { it.status is InstrumentationTest.Status.Ignored },
                failedCount = testsWithPulledFiles.count { it.status is InstrumentationTest.Status.Failed },
                durationNanos = System.nanoTime() - startTimeNanos,
                timestampMillis = System.currentTimeMillis(),
                logcat = logcatFileForDevice(logsDir),
                instrumentationOutput = instrumentationOutputFile
            )
        }

    val testRunFinish = runTests.ofType(Notification.Exit::class.java).cache()

    return Observable
        .zip(adbDeviceTestRun, testRunFinish) { suite, _ -> suite }
        .doOnNext { testRun ->
            println(
                "Test run finished, " +
                        "${testRun.passedCount} passed, " +
                        "${testRun.failedCount} failed, took " +
                        "${testRun.durationNanos.nanosToHumanReadableTime()}."
            )
        }
        .doOnError { println("Error during tests run: $it") }
        .toSingle()
}

private fun logcatFileForDevice(logsDir: File) = File(logsDir, "full.logcat")

private fun logcatFileForTest(logsDir: File, className: String, testName: String): File = File(File(logsDir, className), "$testName.logcat")
