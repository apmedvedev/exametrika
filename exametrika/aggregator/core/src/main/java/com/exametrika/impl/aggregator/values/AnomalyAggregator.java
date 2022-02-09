/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link AnomalyAggregator} is an implementation of {@link IFieldAggregator} for anomaly fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyAggregator implements IFieldAggregator {
    @Override
    public void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context) {
    }
}
