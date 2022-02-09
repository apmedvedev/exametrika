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
import com.exametrika.impl.metrics.exa.probes.ExaInstrumentProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ExaInstrumentProbeConfiguration} is a configuration of exa instrument probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaInstrumentProbeConfiguration extends ProbeConfiguration {
    private final CounterConfiguration transformationTime;
    private final CounterConfiguration beforeTransformationBytes;
    private final CounterConfiguration afterTransformationBytes;
    private final CounterConfiguration skippedClasses;
    private final GaugeConfiguration joinPoints;
    private final LogConfiguration errors;

    public ExaInstrumentProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                           long warmupDelay) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        transformationTime = new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(1000000, 15)), false, 0);
        beforeTransformationBytes = afterTransformationBytes = new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(0, 17)), false, 0);
        skippedClasses = new CounterConfiguration(true, false, 0);
        joinPoints = new GaugeConfiguration(true);
        errors = new LogConfiguration(true, null, Arrays.asList(
                new LogMeterConfiguration(getComponentType() + ".errors.count", new CounterConfiguration(true, true, 0), null,
                        new ErrorCountLogProviderConfiguration())), null, null, 100, 512, 1000, 10, 100);
    }

    public CounterConfiguration getTransformationTime() {
        return transformationTime;
    }

    public CounterConfiguration getBeforeTransformationBytes() {
        return beforeTransformationBytes;
    }

    public CounterConfiguration getAfterTransformationBytes() {
        return afterTransformationBytes;
    }

    public CounterConfiguration getSkippedClasses() {
        return skippedClasses;
    }

    public GaugeConfiguration getJoinPoints() {
        return joinPoints;
    }

    public LogConfiguration getErrors() {
        return errors;
    }

    @Override
    public String getComponentType() {
        return "exa.instrument";
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ExaInstrumentProbe(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component(getComponentType());
        builder.metric(transformationTime.getSchema("exa.instrument.time"));
        builder.metric(beforeTransformationBytes.getSchema("exa.instrument.beforeBytes"));
        builder.metric(afterTransformationBytes.getSchema("exa.instrument.afterBytes"));
        builder.metric(skippedClasses.getSchema("exa.instrument.skipped"));
        builder.metric(joinPoints.getSchema("exa.instrument.joinPoints"));

        List<MetricValueSchemaConfiguration> metrics = errors.getMetricSchemas();
        builder.metrics(metrics);
        components.add(builder.toConfiguration());

        errors.buildComponentSchemas(getComponentType() + ".errors.log", components);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaInstrumentProbeConfiguration))
            return false;

        ExaInstrumentProbeConfiguration configuration = (ExaInstrumentProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
