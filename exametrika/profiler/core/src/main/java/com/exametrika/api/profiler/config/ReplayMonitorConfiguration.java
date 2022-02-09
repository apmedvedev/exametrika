/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.modelling.ReplayMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link ReplayMonitorConfiguration} is a configuration of measurements replay monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ReplayMonitorConfiguration extends MonitorConfiguration {
    private final int nodesCount;
    private final String fileName;
    private final long startPeriod;

    public ReplayMonitorConfiguration(String name, String scope, String measurementStrategy, long period,
                                      int nodesCount, String fileName, long startPeriod) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(fileName);

        this.nodesCount = nodesCount;
        this.fileName = fileName;
        this.startPeriod = startPeriod;
    }

    public int getNodesCount() {
        return nodesCount;
    }

    public String getFileName() {
        return fileName;
    }

    public long getStartPeriod() {
        return startPeriod;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new ReplayMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ReplayMonitorConfiguration))
            return false;

        ReplayMonitorConfiguration configuration = (ReplayMonitorConfiguration) o;
        return super.equals(configuration) && nodesCount == configuration.nodesCount &&
                fileName.equals(configuration.fileName) && startPeriod == configuration.startPeriod;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(nodesCount, fileName, startPeriod);
    }
}
