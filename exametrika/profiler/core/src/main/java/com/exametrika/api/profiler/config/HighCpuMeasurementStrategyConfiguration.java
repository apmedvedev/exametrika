/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.strategies.HighCpuMeasurementStrategy;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link HighCpuMeasurementStrategyConfiguration} is a configuration for high-cpu measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HighCpuMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
    private final long estimationPeriod;
    private final double threshold;

    public HighCpuMeasurementStrategyConfiguration(String name, long estimationPeriod, double threshold) {
        super(name);

        Assert.isTrue(threshold >= 0 && threshold <= 100);

        this.estimationPeriod = estimationPeriod;
        this.threshold = threshold;
    }

    public long getEstimationPeriod() {
        return estimationPeriod;
    }

    public double getThreshold() {
        return threshold;
    }

    @Override
    public IMeasurementStrategy createStrategy() {
        return new HighCpuMeasurementStrategy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HighCpuMeasurementStrategyConfiguration))
            return false;

        HighCpuMeasurementStrategyConfiguration configuration = (HighCpuMeasurementStrategyConfiguration) o;
        return super.equals(configuration) && estimationPeriod == configuration.estimationPeriod && threshold == configuration.threshold;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(estimationPeriod, threshold);
    }
}
