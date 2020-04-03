package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage

interface LogcatListener {
    fun onMessage(device: AndroidDevice, message: LogcatMessage)
    fun onDeviceDisconnected(device: AndroidDevice)
}
