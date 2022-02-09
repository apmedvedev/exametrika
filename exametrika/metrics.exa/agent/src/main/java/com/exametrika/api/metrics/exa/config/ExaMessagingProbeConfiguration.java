/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.meters.config.ErrorCountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.exa.probes.ExaMessagingProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ExaMessagingProbeConfiguration} is a configuration of exa messaging probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaMessagingProbeConfiguration extends ProbeConfiguration {
    private final CounterConfiguration sendBytes;
    private final CounterConfiguration receiveBytes;
    private final LogConfiguration errors;

    public ExaMessagingProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                          long warmupDelay) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        sendBytes = receiveBytes = new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(0, 30)), false, 0);
        errors = new LogConfiguration(true, null, Arrays.asList(
                new LogMeterConfiguration(getComponentType() + ".errors.count", new CounterConfiguration(true, true, 0), null,
                        new ErrorCountLogProviderConfiguration())), null, null, 100, 512, 1000, 10, 100);
    }

    public CounterConfiguration getSendBytes() {
        return sendBytes;
    }

    public CounterConfiguration getReceiveBytes() {
        return receiveBytes;
    }

    public LogConfiguration getErrors() {
        return errors;
    }

    @Override
    public String getComponentType() {
        return "exa.messaging";
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ExaMessagingProbe(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component(getComponentType());
        builder.metric(sendBytes.getSchema("exa.messaging.send.bytes"));
        builder.metric(receiveBytes.getSchema("exa.messaging.receive.bytes"));

        List<MetricValueSchemaConfiguration> metrics = errors.getMetricSchemas();
        builder.metrics(metrics);
        components.add(builder.toConfiguration());

        errors.buildComponentSchemas(getComponentType() + ".errors.log", components);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaMessagingProbeConfiguration))
            return false;

        ExaMessagingProbeConfiguration configuration = (ExaMessagingProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
