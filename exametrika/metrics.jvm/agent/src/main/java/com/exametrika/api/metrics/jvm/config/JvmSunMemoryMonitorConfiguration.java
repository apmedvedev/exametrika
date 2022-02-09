/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.monitors.JvmSunMemoryMonitor;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link JvmSunMemoryMonitorConfiguration} is a configuration for memory monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmSunMemoryMonitorConfiguration extends JvmMemoryMonitorConfiguration {
    private final CounterConfiguration timeCounter;
    private final CounterConfiguration bytesCounter;
    private final LogConfiguration log;
    private final GcFilterConfiguration filter;
    private final long maxGcDuration;

    public JvmSunMemoryMonitorConfiguration(String name, String scope, long period, String measurementStrategy,
                                            CounterConfiguration timeCounter, CounterConfiguration bytesCounter, LogConfiguration log, GcFilterConfiguration filter,
                                            long maxGcDuration) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(timeCounter);
        Assert.notNull(bytesCounter);
        Assert.notNull(log);

        this.timeCounter = timeCounter;
        this.bytesCounter = bytesCounter;
        this.log = log;
        this.filter = filter;
        this.maxGcDuration = maxGcDuration;
    }

    public CounterConfiguration getTimeCounter() {
        return timeCounter;
    }

    public CounterConfiguration getBytesCounter() {
        return bytesCounter;
    }

    public LogConfiguration getLog() {
        return log;
    }

    public GcFilterConfiguration getFilter() {
        return filter;
    }

    public long getMaxGcDuration() {
        return maxGcDuration;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JvmSunMemoryMonitor(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmSunMemoryMonitorConfiguration))
            return false;

        JvmSunMemoryMonitorConfiguration configuration = (JvmSunMemoryMonitorConfiguration) o;
        return super.equals(configuration) && timeCounter.equals(configuration.timeCounter) &&
                bytesCounter.equals(configuration.bytesCounter) && log.equals(configuration.log) &&
                Objects.equals(filter, configuration.filter) && maxGcDuration == configuration.maxGcDuration;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(timeCounter, bytesCounter, log, filter, maxGcDuration);
    }

    @Override
    protected void addPoolMetrics(ComponentValueSchemaBuilder builder, Set<ComponentValueSchemaConfiguration> components) {
        builder.metric(timeCounter.getSchema("jvm.gc.time")).metric(bytesCounter.getSchema("jvm.gc.bytes")).name("jvm.gc.stops");
        builder.metrics(log.getMetricSchemas());
        log.buildComponentSchemas("jvm.gc.log", components);
    }

    @Override
    protected void addCollectorMetrics(ComponentValueSchemaBuilder builder, Set<ComponentValueSchemaConfiguration> components) {
        builder.metric(timeCounter.getSchema("jvm.gc.time")).metric(bytesCounter.getSchema("jvm.gc.bytes")).name("jvm.gc.stops");
    }
}
