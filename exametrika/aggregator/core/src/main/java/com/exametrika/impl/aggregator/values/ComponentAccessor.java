/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link ComponentAccessor} is a component accessor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentAccessor implements IComponentAccessor {
    private IMetricAccessor baseAccessor;
    private int metricIndex;

    public ComponentAccessor(IMetricAccessor baseAccessor, int metricIndex) {
        Assert.notNull(baseAccessor);

        this.baseAccessor = baseAccessor;
        this.metricIndex = metricIndex;
    }

    @Override
    public Object get(IComponentValue value, IComputeContext context) {
        IMetricValue metricValue = value.getMetrics().get(metricIndex);
        return baseAccessor.get(value, metricValue, context);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        return get(componentValue, context);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, context);
    }
}
