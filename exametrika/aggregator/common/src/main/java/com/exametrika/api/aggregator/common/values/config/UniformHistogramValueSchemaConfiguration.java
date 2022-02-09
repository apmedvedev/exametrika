/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.HistogramBuilder;


/**
 * The {@link UniformHistogramValueSchemaConfiguration} is a configuration of uniform histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UniformHistogramValueSchemaConfiguration extends HistogramValueSchemaConfiguration {
    private final long minBound;
    private final long maxBound;
    private final long binWidth;

    public UniformHistogramValueSchemaConfiguration(long minBound, long maxBound, int binCount) {
        super(binCount);

        Assert.isTrue(minBound < maxBound);

        this.minBound = minBound;
        this.maxBound = maxBound;
        binWidth = (maxBound - minBound) / binCount;
    }

    public long getMinBound() {
        return minBound;
    }

    public long getMaxBound() {
        return maxBound;
    }

    @Override
    public void update(HistogramBuilder builder, long value) {
        if (value < minBound)
            builder.setMinOutOfBounds(builder.getMinOutOfBounds() + 1);
        else {
            int binIndex = (int) ((value - minBound) / binWidth);
            if (binIndex < builder.getBinCount())
                builder.getBins()[binIndex]++;
            else
                builder.setMaxOutOfBounds(builder.getMaxOutOfBounds() + 1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UniformHistogramValueSchemaConfiguration))
            return false;

        UniformHistogramValueSchemaConfiguration configuration = (UniformHistogramValueSchemaConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound && maxBound == configuration.maxBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound, maxBound);
    }
}
