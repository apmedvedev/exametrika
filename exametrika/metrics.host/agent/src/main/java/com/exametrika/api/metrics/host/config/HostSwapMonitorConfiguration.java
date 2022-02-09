/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.host.monitors.HostSwapMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostSwapMonitorConfiguration} is a configuration for host swap monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostSwapMonitorConfiguration extends MonitorConfiguration {
    public HostSwapMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostSwapMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.swap")
                .name("host.swap.total")
                .name("host.swap.used")
                .name("host.swap.free")
                .name("host.swap.pagesIn")
                .name("host.swap.pagesOut")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostSwapMonitorConfiguration))
            return false;

        HostSwapMonitorConfiguration configuration = (HostSwapMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
