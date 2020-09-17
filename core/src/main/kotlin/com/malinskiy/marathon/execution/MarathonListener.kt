package com.malinskiy.marathon.execution

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport

interface MarathonListener {
    fun onFinished(report: ExecutionReport)
}
