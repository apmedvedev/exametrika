/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStatisticsValue;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link StatisticsAggregator} is an implementation of {@link IFieldAggregator} for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsAggregator implements IFieldAggregator {
    @Override
    public void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context) {
        IStatisticsValue value = (IStatisticsValue) fieldsToAdd;
        StatisticsBuilder builder = (StatisticsBuilder) fields;

        builder.setSumSquares(builder.getSumSquares() + value.getSumSquares());
    }
}
