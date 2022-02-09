/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.histogram;

import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link LogarithmicHistogramCollector} is an implementation of {@link IFieldCollector} for logarithmic histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class LogarithmicHistogramCollector extends AbstractHistogramCollector {
    private final long minBound;
    private final int startIndex;

    public LogarithmicHistogramCollector(long minBound, int binCount) {
        super(binCount);

        this.minBound = minBound;
        if (minBound >= 1)
            startIndex = Numbers.log2(minBound);
        else
            startIndex = 0;
    }

    @Override
    public void update(long value) {
        if (value < minBound)
            minOutOfBounds++;
        else {
            int binIndex = 0;
            if (value >= 1)
                binIndex = Numbers.log2(value);
            binIndex -= startIndex;

            if (binIndex < bins.length)
                bins[binIndex]++;
            else
                maxOutOfBounds++;
        }

        count++;
    }
}
