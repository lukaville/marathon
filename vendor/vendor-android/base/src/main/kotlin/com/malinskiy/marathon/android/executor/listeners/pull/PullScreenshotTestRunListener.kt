package com.malinskiy.marathon.android.executor.listeners.pull

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.FilteringConfiguration
import com.malinskiy.marathon.execution.matches
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toSafeTestName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newFixedThreadPoolContext
import java.io.File
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class PullScreenshotTestRunListener(
    private val device: AndroidDevice,
    private val devicePoolId: DevicePoolId,
    private val outputDir: File,
    private val testBatch: TestBatch,
    private val parentJob: Job,
    private val pullScreenshotFilterConfiguration: FilteringConfiguration
) : TestRunListener, CoroutineScope {

    private val logger = MarathonLogging.logger("PullScreenshot")

    private val threadPoolDispatcher by lazy {
        newFixedThreadPoolContext(1, "PullScreenshot - ${device.serialNumber}")
    }
    override val coroutineContext: CoroutineContext
        get() = threadPoolDispatcher
    private var screenshotDeferred: Deferred<Unit>? = null
    private var shouldRunPullScreenshot: Boolean = false

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        super.testEnded(test, testMetrics)

        if (!shouldRunPullScreenshot) {
            val testFromBatch = testBatch.tests.first { it.toSafeTestName() == test.toSafeTestName() }
            // We pull the screenshots when the batch is finished, we store if need to after each test
            shouldRunPullScreenshot = pullScreenshotFilterConfiguration.whitelist.any {
                it.matches(testFromBatch)
            }
        }
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        super.testRunEnded(elapsedTime, runMetrics)

        screenshotDeferred?.cancel()
        if (shouldRunPullScreenshot) {
            screenshotDeferred = async(parentJob) {
                pullScreenshots()
            }
        }
        shouldRunPullScreenshot = false
    }


    private fun pullScreenshots() {
        val deviceInfo = device.toDeviceInfo()

        val outputDirectory = Paths.get(
            outputDir.absolutePath,
            OUTPUT_FOLDER_NAME,
            devicePoolId.name,
            deviceInfo.serialNumber,
            testBatch.id
        )
        val remoteFilePath = device.fileManager.remoteScreenshotPath()
        val screenshotsFiles = listOf(
            "metadata.xml",
            "*.png"
        )
        val outputPath = outputDirectory.toFile().absolutePath

        val millis = measureTimeMillis {
            createDirectories(outputDirectory)

            device.fileManager.pullMatchingFilesToDirectory(
                remoteFilePath = remoteFilePath,
                localFilePath = outputPath,
                fileMatch = screenshotsFiles
            )
        }
        logger.trace { "Pulling screenshots finished in ${millis}ms from $remoteFilePath to $outputPath" }

        if (isActive) {
            removeTestScreenshots(screenshotsFiles)
        }
    }

    private fun removeTestScreenshots(testScreenshots: List<String>) {
        val remoteFilePath = device.fileManager.remoteScreenshotPath()
        val millis = measureTimeMillis {
            device.fileManager.removeMatchingFilesFromDirectory(remoteFilePath, testScreenshots)
        }
        logger.trace { "Removed matching screenshots files in ${millis}ms from $remoteFilePath" }
    }

    companion object {
        private const val OUTPUT_FOLDER_NAME = "ui-screenshot"
    }
}
