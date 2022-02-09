/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.profiler.probes.AllocationProbe;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link AllocationProbeConfiguration} is a configuration of allocation probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AllocationProbeConfiguration extends ProbeConfiguration {
    public AllocationProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                        long warmupDelay) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new AllocationProbe(this, context);
    }

    @Override
    public String getComponentType() {
        return null;
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AllocationProbeConfiguration))
            return false;

        AllocationProbeConfiguration configuration = (AllocationProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
