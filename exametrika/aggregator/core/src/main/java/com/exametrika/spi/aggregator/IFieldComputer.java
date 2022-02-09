/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link IFieldComputer} represents a computer of Json representation of field value.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldComputer {
    /**
     * Computes Json representation of the field value.
     *
     * @param componentValue component value of current node
     * @param metricValue    metric value of current node
     * @param value          base field value to get value from or null if current computer does not have corresponding subvalue of root node value
     * @param context        compute context
     * @return field value of one of the supported Json types or null if computer is not applicable in specified context or
     * for specified value
     */
    Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context);

    /**
     * Computes secondary field value based on another field value.
     *
     * @param componentValue component value of current node
     * @param metricValue    metric value of current node
     * @param value          field value to store computation result
     * @param context        compute context
     */
    void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context);
}
