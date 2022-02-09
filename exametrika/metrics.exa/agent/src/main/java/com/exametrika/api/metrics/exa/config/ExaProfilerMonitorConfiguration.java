/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.exa.monitors.ExaProfilerMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link ExaProfilerMonitorConfiguration} is a configuration of exa profiler monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaProfilerMonitorConfiguration extends MonitorConfiguration {
    public ExaProfilerMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new ExaProfilerMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("exa.profiler")
                .object("exa.profiler.dump")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaAgentMonitorConfiguration))
            return false;

        ExaAgentMonitorConfiguration configuration = (ExaAgentMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
