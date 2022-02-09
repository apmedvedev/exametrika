/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.profiler.probes.MethodExitPointProbe;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link MethodExitPointProbeConfiguration} is a configuration of method exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MethodExitPointProbeConfiguration extends ExitPointProbeConfiguration {
    public MethodExitPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                             long warmupDelay) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);
    }

    @Override
    public String getType() {
        return super.getType() + ",local,method";
    }

    @Override
    public String getExitPointType() {
        return "methodRequests";
    }

    @Override
    public String getComponentType() {
        return "app.methodExit";
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isIntermediate() {
        return false;
    }

    @Override
    public boolean isPermanentHotspot() {
        return false;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new MethodExitPointProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MethodExitPointProbeConfiguration))
            return false;

        MethodExitPointProbeConfiguration configuration = (MethodExitPointProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
