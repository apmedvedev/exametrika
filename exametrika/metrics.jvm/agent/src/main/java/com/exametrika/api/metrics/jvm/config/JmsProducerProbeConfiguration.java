/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.JmsProducerProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;

/**
 * The {@link JmsProducerProbeConfiguration} is a configuration of JMS producer probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmsProducerProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration bytesCounter;

    public JmsProducerProbeConfiguration(String name, String scopeType,
                                         String measurementStrategy, long warmupDelay, CounterConfiguration bytesCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);

        Assert.notNull(bytesCounter);

        this.bytesCounter = bytesCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,jms";
    }

    @Override
    public String getExitPointType() {
        return "jmsRequests";
    }

    public CounterConfiguration getBytesCounter() {
        return bytesCounter;
    }

    @Override
    public String getComponentType() {
        return "app.jmsProducer";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public boolean isIntermediate() {
        return true;
    }

    @Override
    public boolean isPermanentHotspot() {
        return true;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new JmsProducerProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (bytesCounter.isEnabled())
            builder.metric(bytesCounter.getSchema("app.jms.bytes"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JmsProducerProbeConfiguration))
            return false;

        JmsProducerProbeConfiguration configuration = (JmsProducerProbeConfiguration) o;
        return super.equals(configuration) && bytesCounter.equals(configuration.bytesCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(bytesCounter);
    }
}
