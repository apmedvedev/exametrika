/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;


/**
 * The {@link LogProbeConfiguration} is a configuration of abstract log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class LogProbeConfiguration extends ProbeConfiguration {
    private final LogConfiguration log;

    public LogProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                 long warmupDelay, LogConfiguration log) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        Assert.notNull(log);

        this.log = log;
    }

    @Override
    public String getComponentType() {
        return "app.log";
    }

    public LogConfiguration getLog() {
        return log;
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
        if (!(o instanceof LogProbeConfiguration))
            return false;

        LogProbeConfiguration configuration = (LogProbeConfiguration) o;
        return super.equals(configuration) && log.equals(configuration.log);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + log.hashCode();
    }
}
