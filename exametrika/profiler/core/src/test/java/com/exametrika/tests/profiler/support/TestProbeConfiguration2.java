/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;

public class TestProbeConfiguration2 extends ProbeConfiguration {
    public TestProbeConfiguration2(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                   long warmupDelay) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new TestProbe2();
    }

    @Override
    public String getComponentType() {
        return null;
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
    }
}