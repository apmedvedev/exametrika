/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.UdpProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.probes.UdpProbe.UdpRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link UdpProbeCollector} is a UDP socket probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UdpProbeCollector extends ExitPointProbeCollector {
    private final UdpProbeConfiguration configuration;
    private ICounter receiveTimeCounter;
    private ICounter receiveBytesCounter;
    private ICounter sendTimeCounter;
    private ICounter sendBytesCounter;

    public UdpProbeCollector(UdpProbeConfiguration configuration,
                             int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                             StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getReceiveTimeCounter().isEnabled())
            receiveTimeCounter = getMeters().addMeter("app.udp.receive.time", configuration.getReceiveTimeCounter(), null);
        if (configuration.getReceiveBytesCounter().isEnabled())
            receiveBytesCounter = getMeters().addMeter("app.udp.receive.bytes", configuration.getReceiveBytesCounter(), null);
        if (configuration.getSendTimeCounter().isEnabled())
            sendTimeCounter = getMeters().addMeter("app.udp.send.time", configuration.getSendTimeCounter(), null);
        if (configuration.getSendBytesCounter().isEnabled())
            sendBytesCounter = getMeters().addMeter("app.udp.send.bytes", configuration.getSendBytesCounter(), null);
    }

    @Override
    protected void doClearMeters() {
        receiveTimeCounter = null;
        receiveBytesCounter = null;
        sendTimeCounter = null;
        sendBytesCounter = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        UdpRawRequest udpRequest = (UdpRawRequest) request;

        if (udpRequest.isReceive()) {
            if (receiveTimeCounter != null)
                receiveTimeCounter.measureDelta(udpRequest.getDelta());
            if (receiveBytesCounter != null)
                receiveBytesCounter.measureDelta(udpRequest.getSize());
        } else {
            if (sendTimeCounter != null)
                sendTimeCounter.measureDelta(udpRequest.getDelta());
            if (sendBytesCounter != null)
                sendBytesCounter.measureDelta(udpRequest.getSize());
        }
    }
}
