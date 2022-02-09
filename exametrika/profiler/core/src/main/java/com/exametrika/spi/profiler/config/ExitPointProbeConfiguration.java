/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ExitPointProbeConfiguration} is a configuration of exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ExitPointProbeConfiguration extends ProbeConfiguration {
    public static final int POINTCUT_PRIORITY = -1000;
    private final RequestMappingStrategyConfiguration requestMappingStrategy;

    public ExitPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                       long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy) {
        super(name, scopeType, 0, measurementStrategy, warmupDelay);

        this.requestMappingStrategy = requestMappingStrategy;
    }

    public final RequestMappingStrategyConfiguration getRequestMappingStrategy() {
        return requestMappingStrategy;
    }

    public String getType() {
        return "exit,jvm," + (isAsync() ? "async," : "sync,") + (isIntermediate() ? "intermediate" : "end");
    }

    /**
     * Returns type name of root exit point collector.
     *
     * @return type name of root exit point collector
     */
    public abstract String getExitPointType();

    /**
     * Is exit point asynchronous or synchronous?
     *
     * @return true if exit point is asynchronous
     */
    public abstract boolean isAsync();

    /**
     * Is exit point intermediate?
     *
     * @return true if exit point is intermediate
     */
    public abstract boolean isIntermediate();

    /**
     * Is exit point collector permanent hotspot?
     *
     * @return true if exit point collector is permanent hotspot
     */
    public abstract boolean isPermanentHotspot();

    public abstract void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                               Set<ComponentValueSchemaConfiguration> components);

    @Override
    public final void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        Assert.supports(false);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExitPointProbeConfiguration))
            return false;

        ExitPointProbeConfiguration configuration = (ExitPointProbeConfiguration) o;
        return super.equals(configuration) && Objects.equals(requestMappingStrategy, configuration.requestMappingStrategy);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(requestMappingStrategy);
    }
}
