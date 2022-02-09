/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.strategies.CompositeMeasurementStrategy;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link CompositeMeasurementStrategyConfiguration} is a configuration for composite measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CompositeMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
    private final boolean allowing;
    private final Type type;
    private final List<MeasurementStrategyConfiguration> strategies;

    public enum Type {
        AND,
        OR
    }

    public CompositeMeasurementStrategyConfiguration(String name, boolean allowing, Type type,
                                                     List<? extends MeasurementStrategyConfiguration> strategies) {
        super(name);

        Assert.notNull(type);
        Assert.notNull(strategies);

        this.allowing = allowing;
        this.type = type;
        this.strategies = Immutables.wrap(strategies);
    }

    public boolean isAllowing() {
        return allowing;
    }

    public Type getType() {
        return type;
    }

    public List<MeasurementStrategyConfiguration> getStrategies() {
        return strategies;
    }

    @Override
    public IMeasurementStrategy createStrategy() {
        return new CompositeMeasurementStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeMeasurementStrategyConfiguration))
            return false;

        CompositeMeasurementStrategyConfiguration configuration = (CompositeMeasurementStrategyConfiguration) o;
        return super.equals(configuration) && allowing == configuration.allowing && type == configuration.type &&
                strategies.equals(configuration.strategies);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowing, type, strategies);
    }
}
