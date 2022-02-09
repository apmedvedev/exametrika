/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.monitors;

import com.exametrika.api.metrics.exa.server.config.ExaServerMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.services.impl.ServiceContainer;
import com.exametrika.impl.boot.Bootstrap;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link ExaServerMonitor} is a monitor of server.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerMonitor extends AbstractMonitor {
    public ExaServerMonitor(ExaServerMonitorConfiguration configuration, IMonitorContext context) {
        super("exa.server", configuration, context, false);
    }

    @Override
    protected void createMeters() {
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
                .put("title", "Exametrika Server")
                .put("description", "The Exametrika server.")
                .toObject();
        meters.setMetadata(metadata);
    }
}
