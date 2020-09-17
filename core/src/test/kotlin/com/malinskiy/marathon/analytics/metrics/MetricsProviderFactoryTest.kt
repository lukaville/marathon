package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.MetricsProviderFactory
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.execution.AnalyticsConfiguration
import com.malinskiy.marathon.test.factory.configuration
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MetricsProviderFactoryTest {

    @Test
    fun shouldReturnNoopProviderWhenDisabled() {
        val configuration = configuration {
            analyticsConfiguration = AnalyticsConfiguration.DisabledAnalytics
        }
        val factory = MetricsProviderFactory(configuration)
        val metricsProvider = factory.create()
        metricsProvider shouldBeInstanceOf NoOpMetricsProvider::class
    }

    @Test
    fun shouldReturnNoopProviderWhenConfigurationIsInvalid() {
        val analyticsConfiguration = AnalyticsConfiguration.InfluxDbConfiguration(
            "host",
            "user",
            "password",
            "db",
            AnalyticsConfiguration.InfluxDbConfiguration.RetentionPolicyConfiguration.default
        )
        val configuration = configuration {
            this.analyticsConfiguration = analyticsConfiguration
        }
        val factory = MetricsProviderFactory(configuration)
        val metricsProvider = factory.create()
        metricsProvider shouldBeInstanceOf NoOpMetricsProvider::class
    }
}
