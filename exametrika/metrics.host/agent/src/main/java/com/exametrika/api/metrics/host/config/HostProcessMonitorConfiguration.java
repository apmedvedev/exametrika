/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.host.monitors.HostProcessMonitor;
import com.exametrika.spi.metrics.host.ProcessNamingStrategyConfiguration;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostProcessMonitorConfiguration} is a configuration for host processes monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostProcessMonitorConfiguration extends MonitorConfiguration {
    private final List<String> filters;
    private final ProcessNamingStrategyConfiguration namingStrategy;

    public HostProcessMonitorConfiguration(String name, String scope, long period, String measurementStrategy, List<String> filters,
                                           ProcessNamingStrategyConfiguration namingStrategy) {
        super(name, scope, period, measurementStrategy);

        this.filters = Immutables.wrap(filters);
        this.namingStrategy = namingStrategy;
    }

    public List<String> getFilters() {
        return filters;
    }

    public ProcessNamingStrategyConfiguration getNamingStrategy() {
        return namingStrategy;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostProcessMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.processes")
                .name("host.processes.total")
                .name("host.processes.idle")
                .name("host.processes.running")
                .name("host.processes.sleeping")
                .name("host.processes.stopped")
                .name("host.processes.threads")
                .name("host.processes.zombie")
                .toConfiguration());
        components.add(ValueSchemas.component("host.process")
                .name("host.process.threads")
                .object("host.process.state")
                .name("host.process.time")
                .name("host.process.cpu.max")
                .name("host.process.cpu.total")
                .name("host.process.cpu.user")
                .name("host.process.cpu.sys")
                .name("host.process.fd")
                .name("host.process.memory.max")
                .name("host.process.memory.total")
                .name("host.process.memory.shared")
                .name("host.process.memory.resident")
                .name("host.process.memory.majorFaults")
                .name("host.process.memory.minorFaults")
                .object("host.process.processor")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostProcessMonitorConfiguration))
            return false;

        HostProcessMonitorConfiguration configuration = (HostProcessMonitorConfiguration) o;
        return super.equals(configuration) && Objects.equals(filters, configuration.filters) &&
                Objects.equals(namingStrategy, configuration.namingStrategy);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(filters, namingStrategy);
    }
}
