/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;


/**
 * The {@link JvmBufferPoolMonitor} is a monitor of buffer pools of JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmBufferPoolMonitor extends AbstractMonitor {
    public static final long maxDirectMemory;

    static {
        long value = -1;
        try {
            value = Memory.getMaxDirectMemory();
        } catch (Throwable e) {
        }

        if (value == -1)
            value = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();

        maxDirectMemory = value;
    }

    public JvmBufferPoolMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, false);
    }

    @Override
    protected void createMeters() {
        for (final BufferPoolMXBean bean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            IMeterContainer meters = createMeterContainer(Names.escape(bean.getName()), MetricName.root(), "jvm.buffer");
            initMetadata(meters);

            meters.addGauge("jvm.memory.buffer.count", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return bean.getCount();
                }
            });
            meters.addGauge("jvm.memory.buffer.used", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = bean.getMemoryUsed();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });
            meters.addGauge("jvm.memory.buffer.total", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return bean.getTotalCapacity();
                }
            });
            meters.addGauge("jvm.memory.buffer.max", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (maxDirectMemory != -1)
                        return maxDirectMemory;
                    else
                        return null;
                }
            });
        }
    }
}
