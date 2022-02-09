/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.profiler.probes.CompositeRequestMappingStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;


/**
 * The {@link CompositeRequestMappingStrategyConfiguration} is a composite request mapping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeRequestMappingStrategyConfiguration extends RequestMappingStrategyConfiguration {
    private final List<RequestMappingStrategyConfiguration> strategies;

    public CompositeRequestMappingStrategyConfiguration(List<? extends RequestMappingStrategyConfiguration> strategies) {
        Assert.notNull(strategies);
        Assert.isTrue(!strategies.isEmpty());

        this.strategies = Immutables.wrap(strategies);
    }

    public List<RequestMappingStrategyConfiguration> getStrategies() {
        return strategies;
    }

    @Override
    public IRequestMappingStrategy createStrategy(IProbeContext context) {
        return new CompositeRequestMappingStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeRequestMappingStrategyConfiguration))
            return false;

        CompositeRequestMappingStrategyConfiguration configuration = (CompositeRequestMappingStrategyConfiguration) o;
        return strategies.equals(configuration.strategies);
    }

    @Override
    public int hashCode() {
        return strategies.hashCode();
    }

    @Override
    public String toString() {
        return strategies.toString();
    }
}
