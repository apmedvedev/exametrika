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
 * The {@link HostSwapMonitor} is a monitor of host swap.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostSwapMonitor extends AbstractMonitor {
    public HostSwapMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super("host.swap", configuration, context, false);
    }

    @Override
    protected void createMeters() {
        meters.addGauge("host.swap.total", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getTotal();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.swap.used", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getUsed();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.swap.free", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getFree();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addCounter("host.swap.pagesIn", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getPageIn();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addCounter("host.swap.pagesOut", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getPageOut();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });
    }
}
