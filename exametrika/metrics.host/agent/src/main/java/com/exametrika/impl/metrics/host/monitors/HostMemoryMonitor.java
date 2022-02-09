/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostMemoryMonitor} is a monitor of host memory metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostMemoryMonitor extends AbstractMonitor {
    public HostMemoryMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super("host.memory", configuration, context, false);
    }

    @Override
    protected void createMeters() {
        meters.addGauge("host.memory.total", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getTotal();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.memory.used", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getActualUsed();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.memory.free", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getActualFree();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.memory.cache", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getUsed() - SigarHolder.instance.getMem().getActualUsed();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });
    }
}
