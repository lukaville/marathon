package com.malinskiy.marathon.execution.progress

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class ProgressReporterSpek : Spek(
    {
        describe("ProgressReporter") {
            val reporter = ProgressReporter()
            val deviceInfo = StubDevice().toDeviceInfo()

            it("should report proper progress for one pool") {
                val poolId = DevicePoolId("testpool")

                val test1 = Test("com.example", "SimpleTest", "method1", emptyList(), TestComponentInfo())
                val test2 = Test("com.example", "SimpleTest", "method2", emptyList(), TestComponentInfo())
                val test3 = Test("com.example", "SimpleTest", "method3", emptyList(), TestComponentInfo())

                reporter.totalTests(poolId, 3)
                reporter.progress().shouldEqualTo(.0f)

                /**
                 * test 1 passed
                 */
                reporter.testStarted(poolId, deviceInfo, test1)
                reporter.testPassed(poolId, deviceInfo, test1)
                reporter.progress().shouldEqualTo(1 / 3f)

                /**
                 * test 2 failed
                 */
                reporter.testStarted(poolId, deviceInfo, test2)
                reporter.testFailed(poolId, deviceInfo, test2)
                reporter.progress().shouldEqualTo(2 / 3f)

                /**
                 * adding 4 retries for test2 and then test 2 passes once
                 */
                reporter.addTests(poolId, 4)
                reporter.progress().shouldEqualTo(2 / 7f)
                reporter.testStarted(poolId, deviceInfo, test2)
                reporter.testPassed(poolId, deviceInfo, test2)
                reporter.progress().shouldEqualTo(3 / 7f)

                /**
                 * 1 retry of test 2 fails
                 */
                reporter.testStarted(poolId, deviceInfo, test2)
                reporter.testFailed(poolId, deviceInfo, test2)
                reporter.progress().shouldEqualTo(4 / 7f)

                /**
                 * 1 retry of test 2 is ignored
                 */
                reporter.testStarted(poolId, deviceInfo, test2)
                reporter.testIgnored(poolId, deviceInfo, test2)
                reporter.progress().shouldEqualTo(5 / 7f)

                /**
                 * removing one retry of test 2
                 */
                reporter.removeTests(poolId, 1)
                reporter.progress().shouldEqualTo(5 / 6f)

                /**
                 * test 3 is ignored (assumption failure or just ignore)
                 */
                reporter.testStarted(poolId, deviceInfo, test3)
                reporter.testIgnored(poolId, deviceInfo, test3)
                val progress = reporter.progress()
                progress.shouldEqualTo(6 / 6f)
            }
        }
    })
