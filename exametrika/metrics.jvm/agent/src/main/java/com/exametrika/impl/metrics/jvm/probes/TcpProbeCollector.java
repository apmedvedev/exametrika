/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.TcpProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.probes.TcpProbe.TcpRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link TcpProbeCollector} is a TCP socket probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TcpProbeCollector extends ExitPointProbeCollector {
    private final TcpProbeConfiguration configuration;
    private ICounter connectTimeCounter;
    private ICounter receiveTimeCounter;
    private ICounter receiveBytesCounter;
    private ICounter sendTimeCounter;
    private ICounter sendBytesCounter;

    public TcpProbeCollector(TcpProbeConfiguration configuration,
                             int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                             StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getConnectTimeCounter().isEnabled())
            connectTimeCounter = getMeters().addMeter("app.tcp.connect.time", configuration.getConnectTimeCounter(), null);
        if (configuration.getReceiveTimeCounter().isEnabled())
            receiveTimeCounter = getMeters().addMeter("app.tcp.receive.time", configuration.getReceiveTimeCounter(), null);
        if (configuration.getReceiveBytesCounter().isEnabled())
            receiveBytesCounter = getMeters().addMeter("app.tcp.receive.bytes", configuration.getReceiveBytesCounter(), null);
        if (configuration.getSendTimeCounter().isEnabled())
            sendTimeCounter = getMeters().addMeter("app.tcp.send.time", configuration.getSendTimeCounter(), null);
        if (configuration.getSendBytesCounter().isEnabled())
            sendBytesCounter = getMeters().addMeter("app.tcp.send.bytes", configuration.getSendBytesCounter(), null);
    }

    @Override
    protected void doClearMeters() {
        connectTimeCounter = null;
        receiveTimeCounter = null;
        receiveBytesCounter = null;
        sendTimeCounter = null;
        sendBytesCounter = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        TcpRawRequest tcpRequest = (TcpRawRequest) request;

        if (tcpRequest.isConnect()) {
            if (connectTimeCounter != null)
                connectTimeCounter.measureDelta(tcpRequest.getDelta());
        } else if (tcpRequest.isReceive()) {
            if (receiveTimeCounter != null)
                receiveTimeCounter.measureDelta(tcpRequest.getDelta());
            if (receiveBytesCounter != null)
                receiveBytesCounter.measureDelta(tcpRequest.getSize());
        } else {
            if (sendTimeCounter != null)
                sendTimeCounter.measureDelta(tcpRequest.getDelta());
            if (sendBytesCounter != null)
                sendBytesCounter.measureDelta(tcpRequest.getSize());
        }
    }
}
