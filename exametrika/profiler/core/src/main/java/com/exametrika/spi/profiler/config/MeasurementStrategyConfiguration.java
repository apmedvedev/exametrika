/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.profiler.IMeasurementStrategy;


/**
 * The {@link MeasurementStrategyConfiguration} is a measurement strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MeasurementStrategyConfiguration extends Configuration {
    private final String name;

    public MeasurementStrategyConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract IMeasurementStrategy createStrategy();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeasurementStrategyConfiguration))
            return false;

        MeasurementStrategyConfiguration configuration = (MeasurementStrategyConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
