/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.histogram;

import java.util.Arrays;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link CustomHistogramCollector} is an implementation of {@link IFieldCollector} for custom histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CustomHistogramCollector extends AbstractHistogramCollector {
    private final long[] bounds;

    public CustomHistogramCollector(long[] bounds) {
        super(bounds.length - 1);

        Assert.notNull(bounds);

        this.bounds = bounds;
    }

    @Override
    public void update(long value) {
        int binIndex = Arrays.binarySearch(bounds, value);
        if (binIndex < 0)
            binIndex = -(binIndex + 2);

        if (binIndex == -1)
            minOutOfBounds++;
        else if (binIndex < bins.length)
            bins[binIndex]++;
        else
            maxOutOfBounds++;

        count++;
    }
}
