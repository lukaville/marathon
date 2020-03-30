package com.malinskiy.marathon.execution

import com.malinskiy.marathon.test.Test

interface StrictRunChecker {
    fun isStrictRun(test: Test): Boolean
}

class ConfigurationStrictRunChecker(private val configuration: Configuration) : StrictRunChecker {

    override fun isStrictRun(test: Test): Boolean =
        configuration.strictMode || configuration.strictRunFilterConfiguration.filter.matches(test)

}
