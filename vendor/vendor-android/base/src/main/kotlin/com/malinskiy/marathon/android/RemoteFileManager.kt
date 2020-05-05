package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.Test

class RemoteFileManager(private val device: AndroidDevice) {
    private val outputDir: String by lazy { device.getExternalStorageMount() }

    fun removeRemotePath(remotePath: String) {
        device.executeCommand("rm $remotePath", "Could not delete remote file(s): $remotePath")
    }

    fun pullFile(remoteFilePath: String, localFilePath: String) {
        device.pullFile(remoteFilePath, localFilePath)
    }

    fun pullMatchingFilesToDirectory(remoteFilePath: String, localFilePath: String, fileMatch: List<String>) {
        val findArgs = fileMatch.toFindArgs()
        val findCommand = "find $remoteFilePath -type f \\($findArgs\\)"

        val filesMatch = device.safeExecuteShellCommand(findCommand)

        filesMatch
            .trimIndent()
            .lines()
            .forEach {
                val fileName = it.replaceBeforeLast('/', "")
                if (fileName.isNotEmpty()) {
                    pullFile(it, "$localFilePath/$fileName")
                }
            }
    }

    fun removeMatchingFilesFromDirectory(remoteFilePath: String, fileMatch: List<String>) {
        val findArgs = fileMatch.toFindArgs()
        val findAndDeleteCommand = "find $remoteFilePath -type f \\($findArgs\\) -delete"

        device.safeExecuteShellCommand(findAndDeleteCommand)
    }

    private fun List<String>.toFindArgs() =
        mapIndexed { index, s ->
            if (index != 0) {
                " -o "
            } else {
                ""
            } + " -name '$s' "
        }.joinToString(separator = "")

    fun createRemoteDirectory() {
        device.executeCommand("mkdir $outputDir", "Could not create remote directory: $outputDir")
    }

    fun removeRemoteDirectory() {
        device.executeCommand("rm -r $outputDir", "Could not delete remote directory: $outputDir")
    }

    fun remoteVideoForTest(test: Test): String {
        return remoteFileForTest(videoFileName(test))
    }

    fun remoteScreenshotPath(): String {
        return "$outputDir/screenshots"
    }

    private fun remoteFileForTest(filename: String): String {
        return "$outputDir/$filename"
    }

    private fun videoFileName(test: Test): String = "${test.pkg}.${test.clazz}-${test.method}.mp4"
}
