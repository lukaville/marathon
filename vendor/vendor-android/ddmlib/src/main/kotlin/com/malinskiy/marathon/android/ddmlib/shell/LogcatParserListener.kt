package com.malinskiy.marathon.android.ddmlib.shell

import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatLongEpochMessageParser
import com.android.ddmlib.logcat.LogCatMessage
import org.apache.commons.io.input.TailerListenerAdapter

class LogcatParserListener(
    private val device: IDevice,
    private val receiver: (List<LogCatMessage>) -> Unit
) : TailerListenerAdapter() {

    private val parser = LogCatLongEpochMessageParser()

    override fun handle(line: String?) {
        if (line != null) {
            val parsedLogcatMessages = parser.processLogLines(arrayOf(line), device)
            receiver.invoke(parsedLogcatMessages)
        }
    }

    override fun handle(exception: Exception) {
        exception.printStackTrace()
    }
}
