/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.monitors.JvmThreadMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link JvmThreadMonitorConfiguration} is a configuration for thread monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmThreadMonitorConfiguration extends MonitorConfiguration {
    private final boolean contention;
    private final boolean locks;
    private final boolean stackTraces;
    private final int maxStackTraceDepth;

    public JvmThreadMonitorConfiguration(String name, String scope, long period, String measurementStrategy,
                                         boolean contention, boolean locks, boolean stackTraces, int maxStackTraceDepth) {
        super(name, scope, period, measurementStrategy);

        this.contention = contention;
        this.locks = locks;
        this.stackTraces = stackTraces;
        this.maxStackTraceDepth = maxStackTraceDepth;
    }

    public boolean isContention() {
        return contention;
    }

    public boolean getLocks() {
        return locks;
    }

    public boolean getStackTraces() {
        return stackTraces;
    }

    public int getMaxStackTraceDepth() {
        return maxStackTraceDepth;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JvmThreadMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("jvm.threads")
                .name("jvm.threads.daemons")
                .name("jvm.threads.started")
                .toConfiguration());
        components.add(ValueSchemas.component("jvm.thread")
                .object("jvm.thread.state")
                .name("jvm.thread.time")
                .name("jvm.thread.cpu.max")
                .name("jvm.thread.cpu.total")
                .name("jvm.thread.cpu.user")
                .name("jvm.thread.cpu.sys")
                .name("jvm.thread.time.waited")
                .name("jvm.thread.time.blocked")
                .object("jvm.thread.lock")
                .object("jvm.thread.locked")
                .object("jvm.thread.stackTrace")
                .name("jvm.thread.allocated")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmThreadMonitorConfiguration))
            return false;

        JvmThreadMonitorConfiguration configuration = (JvmThreadMonitorConfiguration) o;
        return super.equals(configuration) && contention == configuration.contention && locks == configuration.locks &&
                stackTraces == configuration.stackTraces && maxStackTraceDepth == configuration.maxStackTraceDepth;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(contention, locks, stackTraces, maxStackTraceDepth);
    }
}
