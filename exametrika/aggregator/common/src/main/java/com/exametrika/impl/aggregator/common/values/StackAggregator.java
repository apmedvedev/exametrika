/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link StackAggregator} is an aggregator of fields in stack-based metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackAggregator implements IMetricAggregator {
    private final List<IFieldAggregator> fieldAggregators;

    public StackAggregator(List<IFieldAggregator> fieldAggregators) {
        Assert.notNull(fieldAggregators);

        this.fieldAggregators = fieldAggregators;
    }

    @Override
    public void aggregate(IMetricValueBuilder fields, IMetricValue fieldsToAdd, IAggregationContext context) {
        IStackValue value = (IStackValue) fieldsToAdd;
        StackBuilder builder = (StackBuilder) fields;

        aggregate(builder.getInherentFields(), value.getInherentFields(), context);
        if (context.isAllowTotal())
            aggregate(builder.getTotalFields(), value.getTotalFields(), context);
    }

    private void aggregate(List<IFieldValueBuilder> builderFields, List<? extends IFieldValue> valueFields, IAggregationContext context) {
        Assert.isTrue(valueFields.size() <= fieldAggregators.size());
        Assert.isTrue(builderFields.size() == fieldAggregators.size());

        for (int i = 0; i < valueFields.size(); i++) {
            IFieldAggregator fieldAggregator = fieldAggregators.get(i);
            fieldAggregator.aggregate(builderFields.get(i), valueFields.get(i), context);
        }
    }
}
