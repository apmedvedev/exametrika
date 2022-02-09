/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.UdpProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;

/**
 * The {@link UdpProbeConfiguration} is a configuration of UDP socket probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UdpProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration receiveTimeCounter;
    private final CounterConfiguration receiveBytesCounter;
    private final CounterConfiguration sendTimeCounter;
    private final CounterConfiguration sendBytesCounter;

    public UdpProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                 long warmupDelay, CounterConfiguration receiveTimeCounter, CounterConfiguration receiveBytesCounter,
                                 CounterConfiguration sendTimeCounter, CounterConfiguration sendBytesCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);

        Assert.notNull(receiveTimeCounter);
        Assert.notNull(receiveBytesCounter);
        Assert.notNull(sendTimeCounter);
        Assert.notNull(sendBytesCounter);

        this.receiveTimeCounter = receiveTimeCounter;
        this.receiveBytesCounter = receiveBytesCounter;
        this.sendTimeCounter = sendTimeCounter;
        this.sendBytesCounter = sendBytesCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,udp";
    }

    @Override
    public String getExitPointType() {
        return "udpConnections";
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
        return "app.udp";
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
        return new UdpProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (receiveTimeCounter.isEnabled())
            builder.metric(receiveTimeCounter.getSchema("app.udp.receive.time"));
        if (receiveBytesCounter.isEnabled())
            builder.metric(receiveBytesCounter.getSchema("app.udp.receive.bytes"));
        if (sendTimeCounter.isEnabled())
            builder.metric(sendTimeCounter.getSchema("app.udp.send.time"));
        if (sendBytesCounter.isEnabled())
            builder.metric(sendBytesCounter.getSchema("app.udp.send.bytes"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UdpProbeConfiguration))
            return false;

        UdpProbeConfiguration configuration = (UdpProbeConfiguration) o;
        return super.equals(configuration) &&
                receiveTimeCounter.equals(configuration.receiveTimeCounter) &&
                receiveBytesCounter.equals(configuration.receiveBytesCounter) && sendTimeCounter.equals(configuration.sendTimeCounter) &&
                sendBytesCounter.equals(configuration.sendBytesCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(receiveTimeCounter, receiveBytesCounter,
                sendTimeCounter, sendBytesCounter);
    }
}
