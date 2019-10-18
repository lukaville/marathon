package com.malinskiy.marathon.android.executor

import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.ApkParser
import com.malinskiy.marathon.android.InstrumentationInfo
import com.malinskiy.marathon.android.composer.AdbDeviceTest
import com.malinskiy.marathon.android.composer.runTests
import com.malinskiy.marathon.android.safeClearPackage
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toTestName
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.*

val JUNIT_IGNORE_META_PROPERY = MetaProperty("org.junit.Ignore")
const val ERROR_STUCK = "Test got stuck. You can increase the timeout in settings if it's too strict"

class AndroidDeviceTestRunner(private val device: AndroidDevice) {

    companion object {
        private val random = Random(5)
    }

    private val logger = MarathonLogging.logger("AndroidDeviceTestRunner")

    fun execute(
        configuration: Configuration,
        rawTestBatch: TestBatch,
        listener: ITestRunListener
    ) {

        val ignoredTests = rawTestBatch.tests.filter { it.metaProperties.contains(JUNIT_IGNORE_META_PROPERY) }
        val testBatch = TestBatch(rawTestBatch.tests - ignoredTests)

        val androidConfiguration = configuration.vendorConfiguration as AndroidConfiguration
        val info = ApkParser().parseInstrumentationInfo(androidConfiguration.testApplicationOutput)

        try {
            clearData(androidConfiguration, info)
            notifyIgnoredTest(ignoredTests, listener)
            runTestsBlocking(listener, configuration, androidConfiguration, info, testBatch)
        } catch (e: AdbCommandRejectedException) {
            val errorMessage = "adb error while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(e) { errorMessage }
            listener.testRunFailed(errorMessage)
            if (e.isDeviceOffline) {
                throw DeviceLostException(e)
            }
        } catch (e: IOException) {
            val errorMessage = "adb error while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(e) { errorMessage }
            listener.testRunFailed(errorMessage)
        } finally {

        }
    }

    private fun runTestsBlocking(
        listener: ITestRunListener,
        configuration: Configuration,
        androidConfiguration: AndroidConfiguration,
        info: InstrumentationInfo,
        testBatch: TestBatch
    ) {
        val testPackageName = info.instrumentationPackage
        val testRunnerClass = info.testRunnerClass

        val instrumentationArguments = "-e class '${testBatch.tests.joinToString { "${it.pkg}.${it.clazz}#${it.method}" }}'"

        testBatch.tests.forEach {
            listener.testStarted(TestIdentifier("${it.pkg}.${it.clazz}", it.method))
        }

        val result = device
            .runTests(
                testPackageName = testPackageName,
                testRunnerClass = testRunnerClass,
                instrumentationArguments = instrumentationArguments,
                keepOutput = false
            )
            .subscribeOn(Schedulers.io())
            .toBlocking()
            .value()

        result.tests.forEach {
            val testIdentifier = TestIdentifier(it.className, it.testName)
            when (it.status) {
                AdbDeviceTest.Status.Passed -> Unit
                is AdbDeviceTest.Status.Ignored -> listener.testIgnored(testIdentifier)
                is AdbDeviceTest.Status.Failed -> listener.testFailed(testIdentifier, it.status.stacktrace)
            }

            listener.testEnded(testIdentifier, emptyMap())
        }
    }

    private fun notifyIgnoredTest(ignoredTests: List<Test>, listeners: ITestRunListener) {
        ignoredTests.forEach {
            val identifier = it.toTestIdentifier()
            listeners.testStarted(identifier)
            listeners.testIgnored(identifier)
            listeners.testEnded(identifier, hashMapOf())
        }
    }

    private fun clearData(androidConfiguration: AndroidConfiguration, info: InstrumentationInfo) {
        if (androidConfiguration.applicationPmClear) {
            device.ddmsDevice.safeClearPackage(info.applicationPackage)?.also {
                logger.debug { "Package ${info.applicationPackage} cleared: $it" }
            }
        }
        if (androidConfiguration.testApplicationPmClear) {
            device.ddmsDevice.safeClearPackage(info.instrumentationPackage)?.also {
                logger.debug { "Package ${info.applicationPackage} cleared: $it" }
            }
        }
    }
}

internal fun Test.toTestIdentifier(): TestIdentifier = TestIdentifier("$pkg.$clazz", method)
