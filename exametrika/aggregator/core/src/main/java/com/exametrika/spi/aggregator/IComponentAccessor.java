/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;


/**
 * The {@link IComponentAccessor} represents an accessor to component field value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentAccessor extends IMetricAccessor {
    /**
     * Returns field value.
     *
     * @param value   root value of current node
     * @param context compute context
     * @return field value or null if accessor is not applicable in specified context or for specified value
     */
    Object get(IComponentValue value, IComputeContext context);
}
