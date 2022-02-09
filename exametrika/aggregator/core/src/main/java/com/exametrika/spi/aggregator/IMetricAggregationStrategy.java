/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.model.IMetricName;


/**
 * The {@link IMetricAggregationStrategy} represents an aggregation strategy for metrics.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMetricAggregationStrategy {
    /**
     * Returns aggregation hierarchy for specified metric name.
     *
     * @param metric metric name
     * @return aggregation hierarchy
     */
    MetricHierarchy getAggregationHierarchy(IMetricName metric);
}
