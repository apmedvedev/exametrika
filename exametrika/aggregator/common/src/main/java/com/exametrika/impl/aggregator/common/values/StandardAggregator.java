/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link StandardAggregator} is an implementation of {@link IFieldAggregator} for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardAggregator implements IFieldAggregator {
    @Override
    public void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context) {
        IStandardValue value = (IStandardValue) fieldsToAdd;
        StandardBuilder builder = (StandardBuilder) fields;

        builder.setCount(builder.getCount() + value.getCount());

        builder.setMin(Math.min(builder.getMin(), value.getMin()));
        builder.setMax(Math.max(builder.getMax(), value.getMax()));

        builder.setSum(builder.getSum() + value.getSum());
    }
}
