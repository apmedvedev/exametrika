/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


/**
 * The {@link IAggregationService} represents an aggregation service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationService {
    public static final String NAME = "aggregation.AggregationService";

    /**
     * Returns aggregation schema.
     *
     * @return aggregation schema
     */
    IAggregationSchema getAggregationSchema();

    /**
     * Adds period closure listener.
     *
     * @param listener listener
     */
    void addPeriodClosureListener(IPeriodClosureListener listener);

    /**
     * Adds period closure listener.
     *
     * @param listener listener
     */
    void removePeriodClosureListener(IPeriodClosureListener listener);

    /**
     * Aggregates measurements.
     *
     * @param measurements measurements
     */
    void aggregate(MeasurementSet measurements);
}
