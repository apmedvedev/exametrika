/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.AppStackCounterProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;


/**
 * The {@link AppStackCounterConfiguration} is a configuration of application stack counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppStackCounterConfiguration extends StackCounterConfiguration {
    private final AppStackCounterType type;

    public AppStackCounterConfiguration(boolean enabled, AppStackCounterType type) {
        super(enabled);

        Assert.notNull(type);

        this.type = type;
    }

    public AppStackCounterConfiguration(boolean enabled, List<? extends FieldConfiguration> fields, AppStackCounterType type) {
        super(enabled, fields);

        Assert.notNull(type);

        this.type = type;
    }

    public AppStackCounterType getType() {
        return type;
    }

    @Override
    public IMeasurementProvider createProvider(IProbeContext context) {
        return new AppStackCounterProvider(type, context);
    }

    @Override
    public String getMetricType() {
        return type.getMetricType();
    }

    @Override
    public boolean isFast() {
        return !type.isSystem();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppStackCounterConfiguration))
            return false;

        AppStackCounterConfiguration configuration = (AppStackCounterConfiguration) o;
        return super.equals(configuration) && type == configuration.type;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return "jvmStackCounter:" + type;
    }
}
