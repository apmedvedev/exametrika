/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.ICacheable;


/**
 * The {@link IMetricValueBuilder} represents a mutable metric value builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface IMetricValueBuilder extends IMetricValue, ICacheable {
    /**
     * Assigns value to this builder.
     *
     * @param value value to set
     */
    void set(IMetricValue value);

    /**
     * Converts value to immutable implementation of {@link IMetricValue}.
     *
     * @return value
     */
    IMetricValue toValue();

    /**
     * Clears measurement results.
     */
    void clear();
}
