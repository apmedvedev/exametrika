/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link HistogramSerializer} is an implementation of {@link IFieldValueSerializer} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HistogramSerializer implements IFieldValueSerializer {
    private final boolean builder;
    private final int binCount;

    public HistogramSerializer(boolean builder, int binCount) {
        this.builder = builder;
        this.binCount = binCount;
    }

    @Override
    public void serialize(IDataSerialization serialization, IFieldValue value) {
        IHistogramValue histogramValue = (IHistogramValue) value;
        Assert.isTrue(histogramValue.getBinCount() == binCount);

        serialization.writeLong(histogramValue.getMinOutOfBounds());
        serialization.writeLong(histogramValue.getMaxOutOfBounds());

        for (int i = 0; i < binCount; i++)
            serialization.writeLong(histogramValue.getBin(i));
    }

    @Override
    public IFieldValue deserialize(IDataDeserialization deserialization) {
        long minOutOfBounds = deserialization.readLong();
        long maxOutOfBounds = deserialization.readLong();

        long[] bins = new long[binCount];
        for (int i = 0; i < binCount; i++)
            bins[i] = deserialization.readLong();

        if (builder)
            return new HistogramBuilder(bins, minOutOfBounds, maxOutOfBounds);
        else
            return new HistogramValue(bins, minOutOfBounds, maxOutOfBounds);
    }
}
