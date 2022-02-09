/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;


/**
 * The {@link IAggregationContext} represents a context for aggregating component values.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationContext {
    /**
     * Is aggregation of total stack values allowed?
     *
     * @return true if aggregation of total stack values is allowed
     */
    boolean isAllowTotal();

    /**
     * Is aggregation performed on derived measurement?
     *
     * @return true if aggregation is performed on derived measurement
     */
    boolean isDerived();

    /**
     * Is metadata aggregation performed on derived measurement?
     *
     * @return true if metadata aggregation is performed on derived measurement
     */
    boolean isAggregateMetadata();

    /**
     * Returns end aggregation time.
     *
     * @return end aggregation time
     */
    long getTime();

    /**
     * Returns aggregation period.
     *
     * @return aggregation period
     */
    long getPeriod();
}
