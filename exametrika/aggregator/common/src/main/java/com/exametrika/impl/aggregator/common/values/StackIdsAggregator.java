/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link StackIdsAggregator} is an aggregator of stackIds.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackIdsAggregator implements IMetricAggregator {
    @Override
    public void aggregate(IMetricValueBuilder fields, IMetricValue fieldsToAdd, IAggregationContext context) {
        if (context.isDerived())
            return;

        IStackIdsValue value = (IStackIdsValue) fieldsToAdd;
        StackIdsBuilder builder = (StackIdsBuilder) fields;

        builder.addIds(value.getIds());
    }
}
