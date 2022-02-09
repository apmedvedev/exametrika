/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.JdbcProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link JdbcProbeConfiguration} is a configuration of JDBC probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration queryTimeCounter;

    public JdbcProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                  long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy, CounterConfiguration queryCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy);

        Assert.notNull(queryCounter);

        this.queryTimeCounter = queryCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,jdbc";
    }

    @Override
    public String getExitPointType() {
        return "jdbcQueries";
    }

    public CounterConfiguration getQueryTimeCounter() {
        return queryTimeCounter;
    }

    @Override
    public String getComponentType() {
        return "app.jdbc";
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
        return new JdbcProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (queryTimeCounter.isEnabled())
            builder.metric(queryTimeCounter.getSchema("app.db.query.time"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JdbcProbeConfiguration))
            return false;

        JdbcProbeConfiguration configuration = (JdbcProbeConfiguration) o;
        return super.equals(configuration) && queryTimeCounter.equals(configuration.queryTimeCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(queryTimeCounter);
    }
}
