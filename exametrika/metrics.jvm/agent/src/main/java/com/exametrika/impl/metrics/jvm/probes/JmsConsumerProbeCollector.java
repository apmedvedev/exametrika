/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.metrics.jvm.probes.JmsConsumerProbe.JmsConsumerRawRequest;
import com.exametrika.impl.profiler.probes.EntryPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link JmsConsumerProbeCollector} is a JMS consumer probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmsConsumerProbeCollector extends EntryPointProbeCollector {
    public JmsConsumerProbeCollector(int index, JmsConsumerProbe probe, String name, String combineId, ICallPath callPath,
                                     StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        super(index, probe, name, combineId, callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    protected void doEndMeasure(IRequest request, long currentThreadCpuTime) {
        JmsConsumerRawRequest jmsRequest = (JmsConsumerRawRequest) request.getRawRequest();

        if (timeCounter != null)
            timeCounter.measureDelta(jmsRequest.getEndTime() - jmsRequest.getStartTime());
        if (receiveBytesCounter != null)
            receiveBytesCounter.measureDelta(jmsRequest.getSize());
    }
}
