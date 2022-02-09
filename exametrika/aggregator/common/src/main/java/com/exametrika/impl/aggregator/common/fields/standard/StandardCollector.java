/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.standard;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IStandardFieldCollector;


/**
 * The {@link StandardCollector} is an implementation of {@link IFieldCollector} for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardCollector implements IStandardFieldCollector {
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    @Override
    public void update(long value) {
        if (value < min)
            min = value;
        if (value > max)
            max = value;

        sum += value;
        count++;
    }

    @Override
    public void update(long count, long value) {
        if (count == 0)
            return;

        sum += value;
        this.count += count;

        double average = (double) value / count;

        if (average < min)
            min = (long) average;
        if (average > max)
            max = (long) average;
    }

    @Override
    public IFieldValue extract(long count, double approximationMultiplier, boolean clear) {
        if (approximationMultiplier == 0)
            count = this.count;
        else if (count == 0 || count == this.count)
            count = (long) (this.count * approximationMultiplier);

        long sum;
        if (approximationMultiplier > 0)
            sum = (long) (this.sum * approximationMultiplier);
        else
            sum = this.sum;

        IFieldValue value = new StandardValue(count, min, max, sum);

        if (clear) {
            this.min = Long.MAX_VALUE;
            this.max = Long.MIN_VALUE;
            this.sum = 0;
            this.count = 0;
        }

        return value;
    }
}
