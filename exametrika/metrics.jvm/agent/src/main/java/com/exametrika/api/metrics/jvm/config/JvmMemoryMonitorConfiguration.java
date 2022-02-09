/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.jvm.monitors.JvmMemoryMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link JvmMemoryMonitorConfiguration} is a configuration for memory monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmMemoryMonitorConfiguration extends MonitorConfiguration {
    public JvmMemoryMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JvmMemoryMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component("jvm.pool")
                .name("jvm.memory.pool.init")
                .name("jvm.memory.pool.committed")
                .name("jvm.memory.pool.used")
                .name("jvm.memory.pool.max")
                .name("host.process.time");
        addPoolMetrics(builder, components);
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("jvm.gc")
                .name("host.process.time")
                .name("jvm.gc.collectionTime")
                .name("jvm.gc.collectionCount");
        addCollectorMetrics(builder, components);
        components.add(builder.toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmMemoryMonitorConfiguration))
            return false;

        JvmMemoryMonitorConfiguration configuration = (JvmMemoryMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    protected void addPoolMetrics(ComponentValueSchemaBuilder builder, Set<ComponentValueSchemaConfiguration> components) {
        builder.name("jvm.gc.time").name("jvm.gc.bytes").name("jvm.gc.stops");
        components.add(ValueSchemas.component("jvm.gc.log").object("log").toConfiguration());
    }

    protected void addCollectorMetrics(ComponentValueSchemaBuilder builder, Set<ComponentValueSchemaConfiguration> components) {
        builder.name("jvm.gc.time").name("jvm.gc.bytes").name("jvm.gc.stops");
    }
}
