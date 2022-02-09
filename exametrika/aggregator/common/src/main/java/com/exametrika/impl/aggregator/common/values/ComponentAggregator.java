/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link ComponentAggregator} is an aggregator of component.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentAggregator implements IComponentAggregator {
    private final List<IMetricAggregator> metricAggregators;

    public ComponentAggregator(List<IMetricAggregator> metricAggregators) {
        Assert.notNull(metricAggregators);

        this.metricAggregators = metricAggregators;
    }

    @Override
    public void aggregate(IComponentValueBuilder fields, IComponentValue fieldsToAdd, IAggregationContext context) {
        IComponentValue value = fieldsToAdd;
        ComponentBuilder builder = (ComponentBuilder) fields;

        aggregate(builder.getMetrics(), value.getMetrics(), context);

        if (!context.isDerived() && value.getMetadata() != null)
            builder.setMetadata(value.getMetadata());
    }

    private void aggregate(List<IMetricValueBuilder> builderFields, List<? extends IMetricValue> valueFields, IAggregationContext context) {
        Assert.isTrue(builderFields.size() == metricAggregators.size());

        int count = Math.min(valueFields.size(), metricAggregators.size());
        for (int i = 0; i < count; i++) {
            IMetricAggregator metricAggregator = metricAggregators.get(i);
            IMetricValue metric = valueFields.get(i);
            if (metric != null)
                metricAggregator.aggregate(builderFields.get(i), valueFields.get(i), context);
        }
    }
}
