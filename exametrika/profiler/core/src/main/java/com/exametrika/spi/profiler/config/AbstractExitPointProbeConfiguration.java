/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import com.exametrika.impl.profiler.probes.DelegatingExitPointProbe;
import com.exametrika.spi.profiler.AbstractExitPointProbe;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link AbstractExitPointProbeConfiguration} is a configuration of exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractExitPointProbeConfiguration extends ExitPointProbeConfiguration {
    public AbstractExitPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                               long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy);
    }

    @Override
    public final IProbe createProbe(int index, IProbeContext context) {
        return new DelegatingExitPointProbe(this, context, index, getName(), doCreateProbe(context));
    }

    protected abstract AbstractExitPointProbe doCreateProbe(IProbeContext context);
}
