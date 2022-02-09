/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link AnomalyIdAccessor} is an implementation of {@link IFieldAccessor} for anomaly id fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AnomalyIdAccessor implements IFieldAccessor {
    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IAnomalyValue value = (IAnomalyValue) v;
        return value.getId();
    }
}
