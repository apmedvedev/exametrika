/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.host.monitors.HostCurrentProcessMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostCurrentProcessMonitorConfiguration} is a configuration for monitor of current process of host.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostCurrentProcessMonitorConfiguration extends MonitorConfiguration {
    private final String componentType;

    public HostCurrentProcessMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        this(name, scope, period, measurementStrategy, "host.process.current");
    }

    public HostCurrentProcessMonitorConfiguration(String name, String scope, long period, String measurementStrategy, String componentType) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(componentType);

        this.componentType = componentType;
    }

    public String getComponentType() {
        return componentType;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostCurrentProcessMonitor(this, context, componentType);
    }

    @Override
    public final void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component(componentType);
        buildComponentSchema(builder);
        components.add(builder.toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostCurrentProcessMonitorConfiguration))
            return false;

        HostCurrentProcessMonitorConfiguration configuration = (HostCurrentProcessMonitorConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + componentType.hashCode();
    }

    protected void buildComponentSchema(ComponentValueSchemaBuilder builder) {
        builder.name("host.process.threads")
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
                .object("host.process.processor");
    }
}
