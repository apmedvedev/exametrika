/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link HistogramAggregator} is an implementation of {@link IFieldAggregator} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HistogramAggregator implements IFieldAggregator {
    private final int binCount;

    public HistogramAggregator(int binCount) {
        this.binCount = binCount;
    }

    @Override
    public void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context) {
        IHistogramValue value = (IHistogramValue) fieldsToAdd;
        HistogramBuilder builder = (HistogramBuilder) fields;

        Assert.isTrue(value.getBinCount() == binCount);
        Assert.isTrue(builder.getBinCount() == binCount);

        builder.setMinOutOfBounds(builder.getMinOutOfBounds() + value.getMinOutOfBounds());
        builder.setMaxOutOfBounds(builder.getMaxOutOfBounds() + value.getMaxOutOfBounds());

        for (int i = 0; i < value.getBinCount(); i++)
            builder.getBins()[i] += value.getBin(i);
    }
}
