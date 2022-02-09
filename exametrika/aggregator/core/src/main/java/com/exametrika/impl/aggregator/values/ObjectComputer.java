/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ObjectComputer} is an computer of fields in object metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ObjectComputer implements IMetricComputer {
    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        IObjectValue metricValue = (IObjectValue) value;
        if (metricValue != null)
            return metricValue.getObject();
        else
            return null;
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
    }
}
