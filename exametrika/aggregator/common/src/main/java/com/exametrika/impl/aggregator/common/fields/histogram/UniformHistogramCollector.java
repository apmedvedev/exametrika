/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.histogram;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link UniformHistogramCollector} is an implementation of {@link IFieldCollector} for uniform histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class UniformHistogramCollector extends AbstractHistogramCollector {
    private final long minBound;
    private final long binWidth;

    public UniformHistogramCollector(long minBound, long maxBound, int binCount) {
        super(binCount);

        Assert.isTrue(minBound < maxBound);

        this.minBound = minBound;
        binWidth = (maxBound - minBound) / binCount;
    }

    @Override
    public void update(long value) {
        if (value < minBound)
            minOutOfBounds++;
        else {
            int binIndex = (int) ((value - minBound) / binWidth);
            if (binIndex < bins.length)
                bins[binIndex]++;
            else
                maxOutOfBounds++;
        }

        count++;
    }
}
