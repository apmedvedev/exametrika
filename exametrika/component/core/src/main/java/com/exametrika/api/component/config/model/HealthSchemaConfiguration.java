/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;


/**
 * The {@link HealthSchemaConfiguration} is a health schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthSchemaConfiguration extends Configuration {
    private final String firstAggregationPeriod;
    private final CounterConfiguration totalCounter;
    private final CounterConfiguration upCounter;
    private final CounterConfiguration downCounter;
    private final CounterConfiguration failureCounter;
    private final CounterConfiguration maintenanceCounter;

    public HealthSchemaConfiguration(String firstAggregationPeriod, CounterConfiguration totalCounter,
                                     CounterConfiguration upCounter, CounterConfiguration downCounter,
                                     CounterConfiguration failureCounter, CounterConfiguration maintenanceCounter) {
        Assert.notNull(firstAggregationPeriod);
        Assert.notNull(totalCounter);
        Assert.notNull(upCounter);
        Assert.notNull(downCounter);
        Assert.notNull(failureCounter);
        Assert.notNull(maintenanceCounter);

        this.firstAggregationPeriod = firstAggregationPeriod;
        this.totalCounter = totalCounter;
        this.upCounter = upCounter;
        this.downCounter = downCounter;
        this.failureCounter = failureCounter;
        this.maintenanceCounter = maintenanceCounter;
    }

    public String getFirstAggregationPeriod() {
        return firstAggregationPeriod;
    }

    public CounterConfiguration getTotalCounter() {
        return totalCounter;
    }

    public CounterConfiguration getUpCounter() {
        return upCounter;
    }

    public CounterConfiguration getDownCounter() {
        return downCounter;
    }

    public CounterConfiguration getFailureCounter() {
        return failureCounter;
    }

    public CounterConfiguration getMaintenanceCounter() {
        return maintenanceCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HealthSchemaConfiguration))
            return false;

        HealthSchemaConfiguration configuration = (HealthSchemaConfiguration) o;
        return firstAggregationPeriod.equals(configuration.firstAggregationPeriod) &&
                totalCounter.equals(configuration.totalCounter) && upCounter.equals(configuration.upCounter) &&
                downCounter.equals(configuration.downCounter) && failureCounter.equals(configuration.failureCounter) &&
                maintenanceCounter.equals(configuration.maintenanceCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(firstAggregationPeriod, totalCounter, upCounter, downCounter, failureCounter, maintenanceCounter);
    }
}
