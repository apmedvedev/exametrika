/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.host.monitors.HostMemoryMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostMemoryMonitorConfiguration} is a configuration for host memory monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostMemoryMonitorConfiguration extends MonitorConfiguration {
    public HostMemoryMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostMemoryMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.memory")
                .name("host.memory.total")
                .name("host.memory.used")
                .name("host.memory.free")
                .name("host.memory.cache")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostMemoryMonitorConfiguration))
            return false;

        HostMemoryMonitorConfiguration configuration = (HostMemoryMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
