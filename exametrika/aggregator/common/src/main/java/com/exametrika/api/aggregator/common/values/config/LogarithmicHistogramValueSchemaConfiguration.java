/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.HistogramBuilder;


/**
 * The {@link LogarithmicHistogramValueSchemaConfiguration} is a configuration of logarithmic histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogarithmicHistogramValueSchemaConfiguration extends HistogramValueSchemaConfiguration {
    private final long minBound;
    private final int startIndex;

    public LogarithmicHistogramValueSchemaConfiguration(long minBound, int binCount) {
        super(binCount);

        this.minBound = minBound;
        if (minBound >= 1)
            startIndex = Numbers.log2(minBound);
        else
            startIndex = 0;
    }

    public long getMinBound() {
        return minBound;
    }

    @Override
    public void update(HistogramBuilder builder, long value) {
        if (value < minBound)
            builder.setMinOutOfBounds(builder.getMinOutOfBounds() + 1);
        else {
            int binIndex = 0;
            if (value >= 1)
                binIndex = Numbers.log2(value);
            binIndex -= startIndex;

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
        if (!(o instanceof LogarithmicHistogramValueSchemaConfiguration))
            return false;

        LogarithmicHistogramValueSchemaConfiguration configuration = (LogarithmicHistogramValueSchemaConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound);
    }
}
