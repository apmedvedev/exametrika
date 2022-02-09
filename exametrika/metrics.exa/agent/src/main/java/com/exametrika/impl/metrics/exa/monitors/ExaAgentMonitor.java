/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.monitors;

import com.exametrika.api.metrics.exa.config.ExaAgentMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.services.impl.ServiceContainer;
import com.exametrika.impl.boot.Bootstrap;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link ExaAgentMonitor} is a monitor of agent.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaAgentMonitor extends AbstractMonitor {
    public ExaAgentMonitor(ExaAgentMonitorConfiguration configuration, IMonitorContext context) {
        super("exa.agent", configuration, context, false);
        meters.setAlwaysExtractMetadata();
    }

    @Override
    protected void createMeters() {
        meters.addCounter("exa.dummy", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                return 1;
            }
        });

        initMetadata();
    }

    private void initMetadata() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("home", bootstrap.getPlatformHome())
                .put("version", bootstrap.getPlatformVersion())
                .put("bootConfigurationPath", bootstrap.getPlatformBootConfigurationPath())
                .put("serviceConfigurationPath", ((ServiceContainer) bootstrap.getServiceContainer()).getConfigurationPath())
                .put("mode", bootstrap.isAttached() ? "attached" : "started")
                .toObject();
        meters.setMetadata(metadata);
    }
}
