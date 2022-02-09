/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration.Type;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link CompositeMeasurementStrategy} is a composite measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeMeasurementStrategy implements IMeasurementStrategy, ITimerListener {
    private final CompositeMeasurementStrategyConfiguration configuration;
    private final List<IMeasurementStrategy> strategies;

    public CompositeMeasurementStrategy(CompositeMeasurementStrategyConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        List<IMeasurementStrategy> strategies = new ArrayList<IMeasurementStrategy>();
        for (MeasurementStrategyConfiguration strategy : configuration.getStrategies())
            strategies.add(strategy.createStrategy());

        this.strategies = strategies;
    }

    @Override
    public boolean allow() {
        boolean res = configuration.getType() == Type.AND ? true : false;
        for (IMeasurementStrategy strategy : strategies) {
            if (configuration.getType() == Type.AND) {
                res = res && strategy.allow();
                if (!res)
                    break;
            } else {
                res = res || strategy.allow();
                if (res)
                    break;
            }
        }
        return configuration.isAllowing() ? res : !res;
    }

    @Override
    public void onTimer() {
        for (IMeasurementStrategy strategy : strategies) {
            if (strategy instanceof ITimerListener)
                ((ITimerListener) strategy).onTimer();
        }
    }
}
