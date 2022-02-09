/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;


/**
 * The {@link IComponentAggregator} represents a component value aggregator.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentAggregator {
    /**
     * Aggregates specified metric values in given metrics.
     *
     * @param metrics      metrics to add to
     * @param metricsToAdd metrics to add
     * @param context      aggregate context
     */
    void aggregate(IComponentValueBuilder metrics, IComponentValue metricsToAdd, IAggregationContext context);
}
