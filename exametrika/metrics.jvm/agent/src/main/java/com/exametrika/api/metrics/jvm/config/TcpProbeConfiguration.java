/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.TcpProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;

/**
 * The {@link TcpProbeConfiguration} is a configuration of TCP socket probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TcpProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration connectTimeCounter;
    private final CounterConfiguration receiveTimeCounter;
    private final CounterConfiguration receiveBytesCounter;
    private final CounterConfiguration sendTimeCounter;
    private final CounterConfiguration sendBytesCounter;

    public TcpProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                 long warmupDelay, CounterConfiguration connectTimeCounter,
                                 CounterConfiguration receiveTimeCounter, CounterConfiguration receiveBytesCounter, CounterConfiguration sendTimeCounter,
                                 CounterConfiguration sendBytesCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);

        Assert.notNull(connectTimeCounter);
        Assert.notNull(receiveTimeCounter);
        Assert.notNull(receiveBytesCounter);
        Assert.notNull(sendTimeCounter);
        Assert.notNull(sendBytesCounter);

        this.connectTimeCounter = connectTimeCounter;
        this.receiveTimeCounter = receiveTimeCounter;
        this.receiveBytesCounter = receiveBytesCounter;
        this.sendTimeCounter = sendTimeCounter;
        this.sendBytesCounter = sendBytesCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,tcp";
    }

    @Override
    public String getExitPointType() {
        return "tcpConnections";
    }

    public CounterConfiguration getConnectTimeCounter() {
        return connectTimeCounter;
    }

    public CounterConfiguration getReceiveTimeCounter() {
        return receiveTimeCounter;
    }

    public CounterConfiguration getReceiveBytesCounter() {
        return receiveBytesCounter;
    }

    public CounterConfiguration getSendTimeCounter() {
        return sendTimeCounter;
    }

    public CounterConfiguration getSendBytesCounter() {
        return sendBytesCounter;
    }

    @Override
    public String getComponentType() {
        return "app.tcp";
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isIntermediate() {
        return false;
    }

    @Override
    public boolean isPermanentHotspot() {
        return false;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new TcpProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (connectTimeCounter.isEnabled())
            builder.metric(connectTimeCounter.getSchema("app.tcp.connect.time"));
        if (receiveTimeCounter.isEnabled())
            builder.metric(receiveTimeCounter.getSchema("app.tcp.receive.time"));
        if (receiveBytesCounter.isEnabled())
            builder.metric(receiveBytesCounter.getSchema("app.tcp.receive.bytes"));
        if (sendTimeCounter.isEnabled())
            builder.metric(sendTimeCounter.getSchema("app.tcp.send.time"));
        if (sendBytesCounter.isEnabled())
            builder.metric(sendBytesCounter.getSchema("app.tcp.send.bytes"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TcpProbeConfiguration))
            return false;

        TcpProbeConfiguration configuration = (TcpProbeConfiguration) o;
        return super.equals(configuration) && connectTimeCounter.equals(configuration.connectTimeCounter) &&
                receiveTimeCounter.equals(configuration.receiveTimeCounter) &&
                receiveBytesCounter.equals(configuration.receiveBytesCounter) && sendTimeCounter.equals(configuration.sendTimeCounter) &&
                sendBytesCounter.equals(configuration.sendBytesCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(connectTimeCounter, receiveTimeCounter, receiveBytesCounter,
                sendTimeCounter, sendBytesCounter);
    }
}
