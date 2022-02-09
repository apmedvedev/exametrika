/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.monitors;

import com.exametrika.api.metrics.exa.config.ExaProfilerMonitorConfiguration;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.monitors.MonitorContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link ExaProfilerMonitor} is a monitor of profiler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaProfilerMonitor extends AbstractMonitor {
    public ExaProfilerMonitor(ExaProfilerMonitorConfiguration configuration, IMonitorContext context) {
        super("exa.profiler", configuration, context, false);
    }

    @Override
    protected void createMeters() {
        initMetadata();

        meters.addInfo("exa.profiler.dump", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                return ((MonitorContext) context).getProfilingService().dump(IProfilerMXBean.FULL_STATE_FLAG);
            }
        });
    }

    private void initMetadata() {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .toObject();
        meters.setMetadata(metadata);
    }
}
