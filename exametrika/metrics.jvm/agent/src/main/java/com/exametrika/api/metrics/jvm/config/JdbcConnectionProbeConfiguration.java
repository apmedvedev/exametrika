/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.JdbcConnectionProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link JdbcConnectionProbeConfiguration} is a configuration of JDBC connection probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcConnectionProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration connectTimeCounter;

    public JdbcConnectionProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                            long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy, CounterConfiguration connectTimeCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy);

        Assert.notNull(connectTimeCounter);

        this.connectTimeCounter = connectTimeCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,jdbcConnection";
    }

    @Override
    public String getExitPointType() {
        return "jdbcConnections";
    }

    public CounterConfiguration getConnectTimeCounter() {
        return connectTimeCounter;
    }

    @Override
    public String getComponentType() {
        return "app.jdbcConnection";
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
        return new JdbcConnectionProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (connectTimeCounter.isEnabled())
            builder.metric(connectTimeCounter.getSchema("app.db.connect.time"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JdbcConnectionProbeConfiguration))
            return false;

        JdbcConnectionProbeConfiguration configuration = (JdbcConnectionProbeConfiguration) o;
        return super.equals(configuration) && connectTimeCounter.equals(configuration.connectTimeCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(connectTimeCounter);
    }
}
