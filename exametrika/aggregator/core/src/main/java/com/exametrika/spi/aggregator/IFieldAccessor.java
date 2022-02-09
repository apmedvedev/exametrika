/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;


/**
 * The {@link IFieldAccessor} represents an accessor to field value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldAccessor {
    /**
     * Returns field value.
     *
     * @param componentValue root component value of current node
     * @param metricValue    metric value of current node
     * @param value          base field value to get value from or null if current accessor does not have corresponding subvalue of root node value
     * @param context        compute context
     * @return field value or null if accessor is not applicable in specified context or for specified value
     */
    Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context);
}
