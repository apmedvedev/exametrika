/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link ProbeConfiguration} is a configuration of measurement probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ProbeConfiguration extends Configuration {
    private final String name;
    private final String scopeType;
    private final long extractionPeriod;
    private final String measurementStrategy;
    private final long warmupDelay;

    public ProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy, long warmupDelay) {
        Assert.notNull(name);
        Assert.notNull(scopeType);
        Assert.isTrue(extractionPeriod >= 0);

        this.name = name;
        this.scopeType = scopeType;
        this.extractionPeriod = extractionPeriod;
        this.measurementStrategy = measurementStrategy;
        this.warmupDelay = warmupDelay;
    }

    public final String getName() {
        return name;
    }

    public final String getScopeType() {
        return scopeType;
    }

    public final long getExtractionPeriod() {
        return extractionPeriod;
    }

    public final String getMeasurementStrategy() {
        return measurementStrategy;
    }

    public final long getWarmupDelay() {
        return warmupDelay;
    }

    public abstract String getComponentType();

    public abstract void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components);

    public abstract IProbe createProbe(int index, IProbeContext context);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProbeConfiguration))
            return false;

        ProbeConfiguration configuration = (ProbeConfiguration) o;
        return name.equals(configuration.name) && scopeType.equals(configuration.scopeType) && extractionPeriod == configuration.extractionPeriod &&
                Objects.equals(measurementStrategy, configuration.measurementStrategy) && warmupDelay == configuration.warmupDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
    }

    @Override
    public String toString() {
        return name;
    }
}
