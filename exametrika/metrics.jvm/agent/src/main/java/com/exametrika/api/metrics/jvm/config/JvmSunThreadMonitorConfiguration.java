/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.monitors.JvmSunThreadMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link JvmSunThreadMonitorConfiguration} is a configuration for thread monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmSunThreadMonitorConfiguration extends JvmThreadMonitorConfiguration {
    private final boolean memoryAllocation;

    public JvmSunThreadMonitorConfiguration(String name, String scope, long period, String measurementStrategy,
                                            boolean contention, boolean locks, boolean stackTraces, int maxStackTraceDepth, boolean memoryAllocation) {
        super(name, scope, period, measurementStrategy, contention, locks, stackTraces, maxStackTraceDepth);

        this.memoryAllocation = memoryAllocation;
    }

    public boolean isMemoryAllocation() {
        return memoryAllocation;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JvmSunThreadMonitor(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmSunThreadMonitorConfiguration))
            return false;

        JvmSunThreadMonitorConfiguration configuration = (JvmSunThreadMonitorConfiguration) o;
        return super.equals(configuration) && memoryAllocation == configuration.memoryAllocation;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(memoryAllocation);
    }
}
