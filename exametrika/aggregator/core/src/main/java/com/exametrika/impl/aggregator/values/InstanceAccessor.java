/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link InstanceAccessor} is an implementation of {@link IFieldAccessor} for instance fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class InstanceAccessor implements IFieldAccessor {
    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IInstanceValue value = (IInstanceValue) v;
        if (value != null)
            return value.getRecords();
        else
            return null;
    }
}
