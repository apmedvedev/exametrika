/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;


/**
 * The {@link IMetricAccessor} represents an accessor to metric field value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMetricAccessor extends IFieldAccessor {
    /**
     * Returns field value.
     *
     * @param componentValue root component value of current node
     * @param value          metric value of current node
     * @param context        compute context
     * @return field value or null if accessor is not applicable in specified context or for specified value
     */
    Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context);
}
