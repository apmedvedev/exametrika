/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.ManagementFactory;

import com.exametrika.api.metrics.jvm.config.JvmSunThreadMonitorConfiguration;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.IMonitorContext;
import com.sun.management.ThreadMXBean;


/**
 * The {@link JvmSunThreadMonitor} is a monitor of threads of Sun/Oracle JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmSunThreadMonitor extends JvmThreadMonitor {
    private final JvmSunThreadMonitorConfiguration configuration;

    public JvmSunThreadMonitor(JvmSunThreadMonitorConfiguration configuration, IMonitorContext context) {
        super(configuration, context);

        this.configuration = configuration;
    }

    @Override
    public void start() {
        super.start();

        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (configuration.isMemoryAllocation() && bean.isThreadAllocatedMemorySupported() && !bean.isThreadAllocatedMemoryEnabled())
            bean.setThreadAllocatedMemoryEnabled(true);
    }

    @Override
    protected void createAllocatedCounter(IMeterContainer meters, final long threadId) {
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        meters.addCounter("jvm.thread.allocated", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                long value = bean.getThreadAllocatedBytes(threadId);
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });
    }
}
