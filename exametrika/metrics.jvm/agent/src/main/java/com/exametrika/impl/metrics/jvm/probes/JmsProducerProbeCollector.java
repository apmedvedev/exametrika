/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.JmsProducerProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.probes.JmsProducerProbe.JmsProducerRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link JmsProducerProbeCollector} is a JMS producer probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmsProducerProbeCollector extends ExitPointProbeCollector {
    private final JmsProducerProbeConfiguration configuration;
    private ICounter bytesCounter;

    public JmsProducerProbeCollector(JmsProducerProbeConfiguration configuration,
                                     int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                                     StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getBytesCounter().isEnabled())
            bytesCounter = getMeters().addMeter("app.jms.bytes", configuration.getBytesCounter(), null);
    }

    @Override
    protected void doClearMeters() {
        bytesCounter = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        if (bytesCounter != null)
            bytesCounter.measureDelta(((JmsProducerRawRequest) request).getSize());
    }
}
