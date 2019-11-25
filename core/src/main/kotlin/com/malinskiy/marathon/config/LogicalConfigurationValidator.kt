package com.malinskiy.marathon.config

import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.strategy.impl.flakiness.IgnoreFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.ParallelShardingStrategy

class LogicalConfigurationValidator : ConfigurationValidator {
    override fun validate(configuration: Configuration) {
        when {
            configuration.flakinessStrategy !is IgnoreFlakinessStrategy &&
                    configuration.shardingStrategy !is ParallelShardingStrategy -> {
                throw ConfigurationException(
                    "Configuration is invalid: " +
                            "can't use complex sharding and complex flakiness strategy at the same time. " +
                            "See: https://github.com/Malinskiy/marathon/issues/197"
                )
            }
            configuration.flakinessStrategy !is IgnoreFlakinessStrategy &&
                    configuration.cache.isEnabled -> {
                throw ConfigurationException("Complex flakiness strategy and caching is not supported yet")
            }
        }
    }
}
