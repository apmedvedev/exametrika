/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.host.monitors.HostCpuMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostCpuMonitorConfiguration} is a configuration for host cpu monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostCpuMonitorConfiguration extends MonitorConfiguration {
    public HostCpuMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.cpu")
                .name("host.cpu.total")
                .name("host.cpu.idle")
                .name("host.cpu.irq")
                .name("host.cpu.softIrq")
                .name("host.cpu.nice")
                .name("host.cpu.stolen")
                .name("host.cpu.sys")
                .name("host.cpu.user")
                .name("host.cpu.iowait")
                .toConfiguration());
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostCpuMonitor(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostCpuMonitorConfiguration))
            return false;

        HostCpuMonitorConfiguration configuration = (HostCpuMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
