/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.exa.server.monitors.ExaServerMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link ExaServerMonitorConfiguration} is a configuration for server monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerMonitorConfiguration extends MonitorConfiguration {
    public ExaServerMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
        super(name, scope, period, measurementStrategy);
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new ExaServerMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("exa.server").toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerMonitorConfiguration))
            return false;

        ExaServerMonitorConfiguration configuration = (ExaServerMonitorConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
