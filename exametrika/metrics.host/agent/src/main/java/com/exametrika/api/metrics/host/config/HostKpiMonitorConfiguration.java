/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.host.monitors.HostKpiMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostKpiMonitorConfiguration} is a configuration for host runtime monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostKpiMonitorConfiguration extends MonitorConfiguration {
    private final String componentType;
    private final String fileSystemFilter;
    private final String networkInterfaceFilter;

    public HostKpiMonitorConfiguration(String name, String scope, long period, String measurementStrategy,
                                       String componentType, String fileSystemFilter, String networkInterfaceFilter) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(componentType);

        this.componentType = componentType;
        this.fileSystemFilter = fileSystemFilter;
        this.networkInterfaceFilter = networkInterfaceFilter;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getFileSystemFilter() {
        return fileSystemFilter;
    }

    public String getNetworkInterfaceFilter() {
        return networkInterfaceFilter;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostKpiMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component(getComponentType())
                .name("host.cpu.total")
                .name("host.cpu.idle")
                .name("host.cpu.used")
                .name("host.cpu.io")
                .name("host.memory.total")
                .name("host.memory.used")
                .name("host.memory.free")
                .name("host.disk.read")
                .name("host.disk.write")
                .name("host.net.received")
                .name("host.net.sent")
                .name("host.swap.total")
                .name("host.swap.used")
                .name("host.swap.free")
                .name("host.swap.pagesIn")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostKpiMonitorConfiguration))
            return false;

        HostKpiMonitorConfiguration configuration = (HostKpiMonitorConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType) &&
                Objects.equals(fileSystemFilter, configuration.fileSystemFilter) &&
                Objects.equals(networkInterfaceFilter, configuration.networkInterfaceFilter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType, fileSystemFilter, networkInterfaceFilter);
    }
}
