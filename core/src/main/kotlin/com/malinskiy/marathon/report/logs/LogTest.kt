package com.malinskiy.marathon.report.logs

import com.malinskiy.marathon.test.Test

data class LogTest(
    val pkg: String,
    val clazz: String,
    val method: String
)

fun Test.toLogTest(): LogTest =
    LogTest(pkg, clazz, method)
