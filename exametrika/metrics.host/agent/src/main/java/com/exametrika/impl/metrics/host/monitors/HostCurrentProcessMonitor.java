/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.metrics.host.config.HostCurrentProcessMonitorConfiguration;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link HostCurrentProcessMonitor} is a monitor of host processes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostCurrentProcessMonitor extends AbstractMonitor {
    public HostCurrentProcessMonitor(HostCurrentProcessMonitorConfiguration configuration, IMonitorContext context, String componentType) {
        super(componentType, configuration, context, false);
    }

    @Override
    protected void createMeters() {
    }

    @Override
    protected IMeterContainer createMeterContainer(String subScope, IMetricName metricName, String componentType) {
        long processId = SigarHolder.instance.getPid();
        ProcessMeterContainer meterContainer = new ProcessMeterContainer(getMeasurementId(subScope, metricName, componentType),
                context, (IInstanceContextProvider) context, processId);
        addMeters(meterContainer);

        return meterContainer;
    }
}
