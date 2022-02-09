/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;


/**
 * The {@link IMetricComputer} represents a computer of Json representation of metric value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMetricComputer {
    /**
     * Computes Json representation of the field value.
     *
     * @param componentValue root value of current node
     * @param value          metric value
     * @param context        compute context
     * @return field value of one of the supported Json types or null if computer is not applicable in specified context or
     * for specified value
     */
    Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context);

    /**
     * Computes secondary metric value based on another value.
     *
     * @param componentValue root value of current node
     * @param value          metric value
     * @param context        compute context
     */
    void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context);
}
