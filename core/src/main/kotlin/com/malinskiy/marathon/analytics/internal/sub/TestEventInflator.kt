package com.malinskiy.marathon.analytics.internal.sub

interface TestEventInflator {
    fun inflate(event: TestEvent): TestEvent
}
