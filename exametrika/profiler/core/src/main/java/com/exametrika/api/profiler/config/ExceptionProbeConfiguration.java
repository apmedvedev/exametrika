/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.ExceptionProbe;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ExceptionProbeConfiguration} is a configuration of exception probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExceptionProbeConfiguration extends ProbeConfiguration {
    private final LogConfiguration log;

    public ExceptionProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                       long warmupDelay, LogConfiguration log) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        Assert.notNull(log);

        this.log = log;
    }

    public LogConfiguration getLog() {
        return log;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ExceptionProbe(this, context);
    }

    @Override
    public String getComponentType() {
        return "app.exceptions";
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        if (log.isEnabled()) {
            List<MetricValueSchemaConfiguration> metrics = log.getMetricSchemas();
            ComponentValueSchemaBuilder builder = ValueSchemas.component(getComponentType());
            builder.metrics(metrics);
            components.add(builder.toConfiguration());

            log.buildComponentSchemas(getComponentType() + ".log", components);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExceptionProbeConfiguration))
            return false;

        ExceptionProbeConfiguration configuration = (ExceptionProbeConfiguration) o;
        return super.equals(configuration) && log.equals(configuration.log);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + log.hashCode();
    }
}
