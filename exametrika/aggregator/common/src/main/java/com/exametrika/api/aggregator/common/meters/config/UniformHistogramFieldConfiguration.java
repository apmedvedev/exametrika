/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.api.aggregator.common.values.config.UniformHistogramValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link UniformHistogramFieldConfiguration} is a configuration of uniform histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UniformHistogramFieldConfiguration extends HistogramFieldConfiguration {
    private final long minBound;
    private final long maxBound;

    public UniformHistogramFieldConfiguration(long minBound, long maxBound, int binCount) {
        super(binCount);

        Assert.isTrue(minBound < maxBound);

        this.minBound = minBound;
        this.maxBound = maxBound;
    }

    public long getMinBound() {
        return minBound;
    }

    public long getMaxBound() {
        return maxBound;
    }

    @Override
    public FieldValueSchemaConfiguration getSchema() {
        return new UniformHistogramValueSchemaConfiguration(minBound, maxBound, binCount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UniformHistogramFieldConfiguration))
            return false;

        UniformHistogramFieldConfiguration configuration = (UniformHistogramFieldConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound && maxBound == configuration.maxBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound, maxBound);
    }
}
