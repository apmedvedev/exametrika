/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;


import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link MonitorConfiguration} is a configuration of monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MonitorConfiguration extends Configuration {
    private final String name;
    private final String scope;
    private final long period;
    private final String measurementStrategy;

    public MonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        Assert.notNull(name);
        Assert.isTrue(period >= 0);

        this.name = name;
        this.scope = scope;
        this.period = period;
        this.measurementStrategy = measurementStrategy;
    }

    public final String getName() {
        return name;
    }

    public final String getScope() {
        return scope;
    }

    public final long getPeriod() {
        return period;
    }

    public final String getMeasurementStrategy() {
        return measurementStrategy;
    }

    public abstract void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components);

    public abstract IMonitor createMonitor(IMonitorContext context);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MonitorConfiguration))
            return false;

        MonitorConfiguration configuration = (MonitorConfiguration) o;
        return name.equals(configuration.name) && Objects.equals(scope, configuration.scope) && period == configuration.period &&
                Objects.equals(measurementStrategy, configuration.measurementStrategy);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, scope, period, measurementStrategy);
    }

    @Override
    public String toString() {
        return name;
    }
}
