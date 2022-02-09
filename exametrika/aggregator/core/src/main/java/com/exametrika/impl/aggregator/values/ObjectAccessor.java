/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link ObjectAccessor} is an implementation of {@link IFieldAccessor} for object fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectAccessor implements IMetricAccessor {
    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        if (metricValue instanceof IObjectValue)
            return ((IObjectValue) metricValue).getObject();
        else
            return metricValue;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
