package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.TimeoutException
import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceConnected
import com.malinskiy.marathon.device.DeviceProvider.DeviceEvent.DeviceDisconnected
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.time.Timer
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.coroutines.CoroutineContext

private const val DEFAULT_DDM_LIB_TIMEOUT = 30000
private const val DEFAULT_DDM_LIB_SLEEP_TIME = 500

class DdmlibDeviceProvider(
    private val track: Track,
    private val timer: Timer,
    private val androidAppInstaller: AndroidAppInstaller,
    private val fileManager: FileManager,
    private val strictRunChecker: StrictRunChecker,
    private val attachmentManager: AttachmentManager
) : DeviceProvider, CoroutineScope {
    private val logger = MarathonLogging.logger("AndroidDeviceProvider")

    private lateinit var adb: AndroidDebugBridge

    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    private val devices: ConcurrentMap<String, DdmlibAndroidDevice> = ConcurrentHashMap()

    private lateinit var listener: AndroidDebugBridge.IDeviceChangeListener

    private val bootWaitContext = newFixedThreadPoolContext(4, "AndroidDeviceProvider-BootWait")
    override val coroutineContext: CoroutineContext
        get() = bootWaitContext

    override val deviceInitializationTimeoutMillis: Long = 180_000

    override suspend fun initialize(vendorConfiguration: VendorConfiguration) {
        check(vendorConfiguration is AndroidConfiguration) { "Invalid configuration $vendorConfiguration passed" }
        DdmPreferences.setTimeOut(DEFAULT_DDM_LIB_TIMEOUT)
        AndroidDebugBridge.initIfNeeded(false)

        val absolutePath = Paths.get(vendorConfiguration.androidSdk.absolutePath, "platform-tools", "adb").toFile().absolutePath

        listener = object : AndroidDebugBridge.IDeviceChangeListener {
            override fun deviceChanged(device: IDevice?, changeMask: Int) {
                device?.let {
                    launch(context = bootWaitContext) {
                        val maybeNewAndroidDevice =
                            DdmlibAndroidDevice(
                                it,
                                absolutePath,
                                track,
                                timer,
                                androidAppInstaller,
                                attachmentManager,
                                fileManager,
                                vendorConfiguration.serialStrategy,
                                strictRunChecker
                            )
                        val healthy = maybeNewAndroidDevice.healthy

                        logger.debug { "Device ${device.serialNumber} changed state. Healthy = $healthy" }
                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        } else {
                            //This shouldn't have any side effects even if device was previously removed
                            notifyDisconnected(maybeNewAndroidDevice)
                        }
                    }
                }
            }

            override fun deviceConnected(device: IDevice?) {
                device?.let {
                    launch {
                        val maybeNewAndroidDevice = DdmlibAndroidDevice(
                            ddmsDevice = it,
                            track = track,
                            timer = timer,
                            serialStrategy = vendorConfiguration.serialStrategy,
                            androidAppInstaller = androidAppInstaller,
                            attachmentManager = attachmentManager,
                            reportsFileManager = fileManager,
                            adbPath = absolutePath,
                            strictRunChecker = strictRunChecker
                        )

                        val healthy = maybeNewAndroidDevice.healthy
                        logger.debug("Device ${maybeNewAndroidDevice.serialNumber} connected. Healthy = $healthy")

                        if (healthy) {
                            verifyBooted(maybeNewAndroidDevice)
                            val androidDevice = getDeviceOrPut(maybeNewAndroidDevice)
                            notifyConnected(androidDevice)
                        }
                    }
                }
            }

            override fun deviceDisconnected(device: IDevice?) {
                device?.let {
                    launch {
                        logger.debug { "Device ${device.serialNumber} disconnected" }
                        matchDdmsToDevice(it)?.let {
                            notifyDisconnected(it)
                            it.dispose()
                        }
                    }
                }
            }

            private suspend fun verifyBooted(device: DdmlibAndroidDevice) {
                if (!waitForBoot(device)) throw TimeoutException("Timeout waiting for device ${device.serialNumber} to boot")
            }

            private suspend fun waitForBoot(device: DdmlibAndroidDevice): Boolean {
                var booted = false

                track.trackProviderDevicePreparing(device) {
                    for (i in 1..30) {
                        if (device.booted) {
                            logger.debug { "Device ${device.serialNumber} booted!" }
                            booted = true
                            break
                        } else {
                            delay(1000)
                            logger.debug { "Device ${device.serialNumber} is still booting..." }
                        }

                        if (Thread.interrupted() || !isActive) {
                            booted = true
                            break
                        }
                    }
                }

                return booted
            }

            private fun notifyConnected(device: DdmlibAndroidDevice) {
                launch {
                    channel.send(DeviceConnected(device))
                }
            }

            private fun notifyDisconnected(device: DdmlibAndroidDevice) {
                launch {
                    androidAppInstaller.onDisconnected(device)
                    channel.send(DeviceDisconnected(device))
                }
            }
        }
        AndroidDebugBridge.addDeviceChangeListener(listener)
        adb = AndroidDebugBridge.createBridge(absolutePath, false)

        var getDevicesCountdown = DEFAULT_DDM_LIB_TIMEOUT
        val sleepTime = DEFAULT_DDM_LIB_SLEEP_TIME
        while (!adb.hasInitialDeviceList() || !adb.hasDevices() && getDevicesCountdown >= 0) {
            try {
                Thread.sleep(sleepTime.toLong())
            } catch (e: InterruptedException) {
                throw TimeoutException("Timeout getting device list", e)
            }
            getDevicesCountdown -= sleepTime
        }

        adb.devices.forEach {
            listener.deviceConnected(it)
        }

        if (!adb.hasInitialDeviceList() || !adb.hasDevices()) {
            throw NoDevicesException("No devices found.")
        }
    }

    private fun getDeviceOrPut(androidDevice: DdmlibAndroidDevice): DdmlibAndroidDevice {
        val newAndroidDevice = devices.getOrPut(androidDevice.serialNumber) {
            androidDevice
        }
        if (newAndroidDevice != androidDevice) {
            androidDevice.dispose()
        }

        return newAndroidDevice
    }

    private fun matchDdmsToDevice(device: IDevice): DdmlibAndroidDevice? {
        val observedDevices = devices.values
        return observedDevices.findLast {
            device == it.ddmsDevice ||
                device.serialNumber == it.ddmsDevice.serialNumber
        }
    }

    private fun AndroidDebugBridge.hasDevices(): Boolean = devices.isNotEmpty()

    override suspend fun terminate() {
        if (::listener.isInitialized) {
            AndroidDebugBridge.removeDeviceChangeListener(listener)
        }
        bootWaitContext.close()
        channel.close()
    }

    override fun subscribe() = channel

}
