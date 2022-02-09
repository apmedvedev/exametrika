/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.histogram;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link AbstractHistogramCollector} is an abstract implementation of {@link IFieldCollector} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractHistogramCollector implements IFieldCollector {
    protected long[] bins;
    protected long minOutOfBounds;
    protected long maxOutOfBounds;
    protected long count;

    public AbstractHistogramCollector(int binCount) {
        Assert.isTrue(binCount > 0);

        bins = new long[binCount];
    }

    @Override
    public final IFieldValue extract(long count, double approximationMultiplier, boolean clear) {
        approximationMultiplier = correctApproximationMultiplier(count, approximationMultiplier);

        long minOutOfBounds;
        long maxOutOfBounds;
        long[] bins;
        if (approximationMultiplier > 0) {
            minOutOfBounds = (long) (this.minOutOfBounds * approximationMultiplier);
            maxOutOfBounds = (long) (this.maxOutOfBounds * approximationMultiplier);
            bins = new long[this.bins.length];
            System.arraycopy(this.bins, 0, bins, 0, bins.length);

            for (int i = 0; i < bins.length; i++)
                bins[i] *= approximationMultiplier;
            minOutOfBounds *= approximationMultiplier;
            maxOutOfBounds *= approximationMultiplier;
        } else {
            minOutOfBounds = this.minOutOfBounds;
            maxOutOfBounds = this.maxOutOfBounds;
            bins = new long[this.bins.length];
            System.arraycopy(this.bins, 0, bins, 0, bins.length);
        }

        IFieldValue value = new HistogramValue(bins, minOutOfBounds, maxOutOfBounds);

        if (clear) {
            this.bins = new long[bins.length];
            this.minOutOfBounds = 0;
            this.maxOutOfBounds = 0;
            this.count = 0;
        }

        return value;
    }

    private double correctApproximationMultiplier(long count, double approximationMultiplier) {
        if (approximationMultiplier == 0)
            return 0;
        else if (count == 0 || count == this.count)
            return approximationMultiplier;
        else
            return (double) count / this.count;
    }
}
