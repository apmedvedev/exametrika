/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.fields;

import com.exametrika.api.aggregator.common.values.IComponentValue;


/**
 * The {@link IAggregationRecord} represents an aggregation record.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAggregationRecord {
    /**
     * Returns aggregated value.
     *
     * @return aggregated value
     */
    IComponentValue getValue();

    /**
     * Returns end time of aggregation.
     *
     * @return end time of aggregation
     */
    long getTime();

    /**
     * Returns period of aggregation.
     *
     * @return period of aggregation
     */
    long getPeriod();
}
