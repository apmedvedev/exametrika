/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.Arrays;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.api.aggregator.common.values.config.HistogramValueSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link HistogramBuilder} is a measurement data for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class HistogramBuilder implements IFieldValueBuilder, IHistogramValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(HistogramBuilder.class) + CacheSizes.ARRAY_CACHE_SIZE;
    private final long[] bins;
    private long minOutOfBounds;
    private long maxOutOfBounds;

    public HistogramBuilder(int binCount) {
        this(new long[binCount], 0, 0);
    }

    public HistogramBuilder(long[] bins, long minOutOfBounds, long maxOutOfBounds) {
        Assert.notNull(bins);

        this.bins = bins;
        this.minOutOfBounds = minOutOfBounds;
        this.maxOutOfBounds = maxOutOfBounds;
    }

    @Override
    public int getBinCount() {
        return bins.length;
    }

    @Override
    public long getBin(int index) {
        return bins[index];
    }

    public long[] getBins() {
        return bins;
    }

    @Override
    public long getMinOutOfBounds() {
        return minOutOfBounds;
    }

    public void setMinOutOfBounds(long minOutOfBounds) {
        this.minOutOfBounds = minOutOfBounds;
    }

    @Override
    public long getMaxOutOfBounds() {
        return maxOutOfBounds;
    }

    public void setMaxOutOfBounds(long maxOutOfBounds) {
        this.maxOutOfBounds = maxOutOfBounds;
    }

    @Override
    public JsonObject toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IFieldValue value) {
        Assert.notNull(value);

        IHistogramValue histogramValue = (IHistogramValue) value;
        Assert.isTrue(bins.length == histogramValue.getBinCount());

        for (int i = 0; i < bins.length; i++)
            bins[i] = histogramValue.getBin(i);

        minOutOfBounds = histogramValue.getMinOutOfBounds();
        maxOutOfBounds = histogramValue.getMaxOutOfBounds();
    }

    @Override
    public HistogramValue toValue() {
        long[] bins = new long[this.bins.length];
        System.arraycopy(this.bins, 0, bins, 0, this.bins.length);
        return new HistogramValue(bins, minOutOfBounds, maxOutOfBounds);
    }

    @Override
    public void clear() {
        Arrays.fill(bins, 0);
        minOutOfBounds = 0;
        maxOutOfBounds = 0;
    }

    @Override
    public void normalizeEnd(long count) {
    }

    @Override
    public void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum) {
        clear();

        ((HistogramValueSchemaConfiguration) fieldSchemaConfiguration).update(this, sum);
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE + bins.length * 8;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HistogramBuilder))
            return false;

        HistogramBuilder data = (HistogramBuilder) o;
        return Arrays.equals(bins, data.bins) && minOutOfBounds == data.minOutOfBounds && maxOutOfBounds == data.maxOutOfBounds;
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(bins) + Objects.hashCode(minOutOfBounds, maxOutOfBounds);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
