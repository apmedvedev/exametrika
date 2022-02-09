/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.profiler.probes.ThreadExitPointProbe;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link ThreadExitPointProbeConfiguration} is a configuration of thread exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadExitPointProbeConfiguration extends ExitPointProbeConfiguration {
    public ThreadExitPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                             long warmupDelay) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);
    }

    @Override
    public String getType() {
        return super.getType() + ",local,thread";
    }

    @Override
    public String getExitPointType() {
        return "threadRequests";
    }

    @Override
    public String getComponentType() {
        return "app.threadExit";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public boolean isIntermediate() {
        return true;
    }

    @Override
    public boolean isPermanentHotspot() {
        return true;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ThreadExitPointProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ThreadExitPointProbeConfiguration))
            return false;

        ThreadExitPointProbeConfiguration configuration = (ThreadExitPointProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
