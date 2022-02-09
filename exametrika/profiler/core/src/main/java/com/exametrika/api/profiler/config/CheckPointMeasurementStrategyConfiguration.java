/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.strategies.CheckPointMeasurementStrategy;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link CheckPointMeasurementStrategyConfiguration} is a configuration for checkpoint measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CheckPointMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
    private final boolean allowing;

    public CheckPointMeasurementStrategyConfiguration(String name, boolean allowing) {
        super(name);

        this.allowing = allowing;
    }

    public boolean isAllowing() {
        return allowing;
    }

    @Override
    public IMeasurementStrategy createStrategy() {
        return new CheckPointMeasurementStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CheckPointMeasurementStrategyConfiguration))
            return false;

        CheckPointMeasurementStrategyConfiguration configuration = (CheckPointMeasurementStrategyConfiguration) o;
        return super.equals(configuration) && allowing == configuration.allowing;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowing);
    }
}
