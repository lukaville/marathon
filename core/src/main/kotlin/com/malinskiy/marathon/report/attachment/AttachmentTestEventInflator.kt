package com.malinskiy.marathon.report.attachment

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEventInflator
import com.malinskiy.marathon.io.AttachmentManager

class AttachmentTestEventInflator(
    private val attachmentManager: AttachmentManager
) : TestEventInflator {

    /**
     * Writes test attachments to the destination directory
     */
    override fun inflate(event: TestEvent): TestEvent {
        val test = event.testResult
        val newAttachments = event.testResult.attachments
            .map { attachment ->
                val targetFile = attachmentManager.writeToTarget(
                    test.batchId,
                    event.poolId,
                    test.device,
                    test.test,
                    attachment
                )

                attachment.copy(file = targetFile)
            }

        return event.copy(
            testResult = event.testResult.copy(
                attachments = newAttachments
            )
        )
    }
}
