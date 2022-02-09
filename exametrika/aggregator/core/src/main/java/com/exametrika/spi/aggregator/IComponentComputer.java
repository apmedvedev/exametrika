/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;


/**
 * The {@link IComponentComputer} represents a computer of Json representation of component value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentComputer {
    /**
     * Computes Json representation of the field value.
     *
     * @param value           root value of current node
     * @param context         compute context
     * @param includeTime     if true include aggregation time and period
     * @param includeMetadata if true metadata are included
     * @return field value of one of the supported Json types or null if computer is not applicable in specified context or
     * for specified value
     */
    Object compute(IComponentValue value, IComputeContext context, boolean includeTime, boolean includeMetadata);

    /**
     * Computes secondary component value based on another value.
     *
     * @param value   root value of current node
     * @param context compute context
     */
    void computeSecondary(IComponentValue value, IComputeContext context);
}
