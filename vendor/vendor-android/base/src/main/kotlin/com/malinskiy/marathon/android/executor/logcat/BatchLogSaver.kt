package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogTest
import java.io.File
import java.io.Writer
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BatchLogSaver {

    private val logSaver = LogSaver()
    private val testLogSavers: MutableMap<LogTest, LogSaver> = hashMapOf()

    fun save(entry: SaveEntry, test: LogTest?) {
        logSaver.saveEntry(entry)

        if (test != null) {
            val testSaver = testLogSavers.getOrPut(test) { LogSaver() }
            testSaver.saveEntry(entry)
        }
    }

    fun close(test: LogTest) {
        testLogSavers[test]?.close()
    }

    fun close() {
        testLogSavers.values.forEach(LogSaver::close)
        logSaver.close()
    }

    fun createBatchLogs(): BatchLogs {
        val tests = testLogSavers
            .mapValues { it.value.createLog() }

        return BatchLogs(
            tests = tests,
            log = logSaver.createLog()
        )
    }

    sealed class SaveEntry {
        class Message(val message: LogcatMessage) : SaveEntry()
        class Event(val event: LogEvent) : SaveEntry()
    }

    private class LogSaver {

        private val logFile: File = createTempFile()
            .also { it.deleteOnExit() }

        private val fileWriter: Writer = logFile.bufferedWriter()
        private val events: MutableList<LogEvent> = arrayListOf()
        private var isClosed = false

        fun saveEntry(entry: SaveEntry) {
            if (isClosed) return

            when (entry) {
                is SaveEntry.Message -> fileWriter.writeMessage(entry.message)
                is SaveEntry.Event -> events.add(entry.event)
            }
        }

        fun close() {
            isClosed = true
            fileWriter.close()
        }

        fun createLog(): Log {
            close()
            return Log(logFile, events)
        }

        private fun Writer.writeMessage(logcatMessage: LogcatMessage) {
            val timeStamp = LOGCAT_TIMESTAMP_FORMATTER.format(logcatMessage.timestamp)
            write("$timeStamp ${logcatMessage.processId}-${logcatMessage.threadId}/${logcatMessage.applicationName} ${logcatMessage.logLevel.letter}/${logcatMessage.tag}: ${logcatMessage.body}\n")
        }

        private companion object {
            private val LOGCAT_TIMESTAMP_FORMATTER = DateTimeFormatter
                .ofPattern("MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
        }
    }
}
