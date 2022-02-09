/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link ObjectAggregator} is an implementation of {@link IFieldAggregator} for object fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectAggregator implements IMetricAggregator {
    @Override
    public void aggregate(IMetricValueBuilder fields, IMetricValue fieldsToAdd, IAggregationContext context) {
        if (context.isDerived())
            return;

        IObjectValue value = (IObjectValue) fieldsToAdd;
        ObjectBuilder builder = (ObjectBuilder) fields;

        builder.setObject(value.getObject());
    }
}
