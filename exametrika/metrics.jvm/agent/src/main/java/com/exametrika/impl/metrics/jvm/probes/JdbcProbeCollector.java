/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.JdbcProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.probes.JdbcProbe.JdbcRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link JdbcProbeCollector} is a JDBC probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcProbeCollector extends ExitPointProbeCollector {
    private final JdbcProbeConfiguration configuration;
    private ICounter queryTimeCounter;

    public JdbcProbeCollector(JdbcProbeConfiguration configuration,
                              int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                              StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getQueryTimeCounter().isEnabled())
            queryTimeCounter = getMeters().addMeter("app.db.query.time", configuration.getQueryTimeCounter(), null);
    }

    @Override
    protected void doClearMeters() {
        queryTimeCounter = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        JdbcRawRequest dbRequest = request.getRawRequest();
        if (queryTimeCounter != null)
            queryTimeCounter.measureDelta(dbRequest.getDelta());
    }
}
