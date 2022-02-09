/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import com.exametrika.api.profiler.config.CheckPointMeasurementStrategyConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.boot.CheckPointMeasurementStrategyInterceptor;
import com.exametrika.spi.profiler.IMeasurementStrategy;


/**
 * The {@link CheckPointMeasurementStrategy} is a checkpoint measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CheckPointMeasurementStrategy implements IMeasurementStrategy {
    private final CheckPointMeasurementStrategyConfiguration configuration;

    public CheckPointMeasurementStrategy(CheckPointMeasurementStrategyConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean allow() {
        boolean allowed = CheckPointMeasurementStrategyInterceptor.allowed;
        return configuration.isAllowing() ? allowed : !allowed;
    }
}
