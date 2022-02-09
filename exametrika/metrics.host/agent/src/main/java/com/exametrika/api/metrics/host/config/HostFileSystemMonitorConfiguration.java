/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.host.monitors.HostFileSystemMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostFileSystemMonitorConfiguration} is a configuration for host file system monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostFileSystemMonitorConfiguration extends MonitorConfiguration {
    private final String filter;

    public HostFileSystemMonitorConfiguration(String name, String scope, long period, String measurementStrategy, String filter) {
        super(name, scope, period, measurementStrategy);

        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostFileSystemMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.fs")
                .name("host.disk.read")
                .name("host.disk.write")
                .name("host.disk.serviceTime")
                .name("host.disk.queue")
                .name("host.disk.total")
                .name("host.disk.used")
                .name("host.disk.free")
                .name("host.disk.available")
                .name("host.fs.files")
                .name("host.fs.freeFiles")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostFileSystemMonitorConfiguration))
            return false;

        HostFileSystemMonitorConfiguration configuration = (HostFileSystemMonitorConfiguration) o;
        return super.equals(configuration) && Objects.equals(filter, configuration.filter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(filter);
    }
}
