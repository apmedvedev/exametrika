/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.strategies.ExternalMeasurementStrategy;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link ExternalMeasurementStrategyConfiguration} is a configuration for external measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExternalMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
    private final boolean enabled;
    private final long warmupDelay;

    public ExternalMeasurementStrategyConfiguration(String name, boolean enabled, long warmupDelay) {
        super(name);

        this.enabled = enabled;
        this.warmupDelay = warmupDelay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getWarmupDelay() {
        return warmupDelay;
    }

    @Override
    public IMeasurementStrategy createStrategy() {
        return new ExternalMeasurementStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExternalMeasurementStrategyConfiguration))
            return false;

        ExternalMeasurementStrategyConfiguration configuration = (ExternalMeasurementStrategyConfiguration) o;
        return super.equals(configuration) && enabled == configuration.enabled && warmupDelay == configuration.warmupDelay;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(enabled, warmupDelay);
    }
}
