/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.metrics.host.config.HostCurrentProcessMonitorConfiguration;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.monitors.JvmKpiMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link JvmKpiMonitorConfiguration} is a configuration for KPI monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmKpiMonitorConfiguration extends HostCurrentProcessMonitorConfiguration {
    private final long maxGcDuration;

    public JvmKpiMonitorConfiguration(String name, String scope, String componentType, long period, String measurementStrategy,
                                      long maxGcDuration) {
        super(name, scope, period, measurementStrategy, componentType);

        this.maxGcDuration = maxGcDuration;
    }

    public long getMaxGcDuration() {
        return maxGcDuration;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JvmKpiMonitor(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmKpiMonitorConfiguration))
            return false;

        JvmKpiMonitorConfiguration configuration = (JvmKpiMonitorConfiguration) o;
        return super.equals(configuration) && maxGcDuration == configuration.maxGcDuration;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(maxGcDuration);
    }

    @Override
    protected void buildComponentSchema(ComponentValueSchemaBuilder builder) {
        super.buildComponentSchema(builder);

        builder.name("jvm.threads.total")
                .name("jvm.memory.heap.init")
                .name("jvm.memory.heap.committed")
                .name("jvm.memory.heap.used")
                .name("jvm.memory.heap.max")
                .name("jvm.memory.nonHeap.init")
                .name("jvm.memory.nonHeap.committed")
                .name("jvm.memory.nonHeap.used")
                .name("jvm.memory.nonHeap.max")
                .name("jvm.memory.buffer.used")
                .name("jvm.memory.buffer.total")
                .name("jvm.memory.buffer.max")
                .name("jvm.gc.collectionTime")
                .name("jvm.gc.stops");
    }
}
