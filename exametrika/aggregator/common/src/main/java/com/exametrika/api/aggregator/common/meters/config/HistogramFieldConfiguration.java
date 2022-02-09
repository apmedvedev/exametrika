/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.fields.histogram.HistogramFieldFactory;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;


/**
 * The {@link HistogramFieldConfiguration} is a configuration of histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class HistogramFieldConfiguration extends FieldConfiguration {
    protected final int binCount;

    public HistogramFieldConfiguration(int binCount) {
        Assert.isTrue(binCount > 0);

        this.binCount = binCount;
    }

    public int getBinCount() {
        return binCount;
    }

    @Override
    public IFieldFactory createFactory() {
        return new HistogramFieldFactory(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HistogramFieldConfiguration))
            return false;

        HistogramFieldConfiguration configuration = (HistogramFieldConfiguration) o;
        return binCount == configuration.binCount;
    }

    @Override
    public int hashCode() {
        return 31 * binCount;
    }

    @Override
    public String toString() {
        return "histogram";
    }
}
