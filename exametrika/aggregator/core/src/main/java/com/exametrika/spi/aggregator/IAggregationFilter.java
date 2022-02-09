/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;


/**
 * The {@link IAggregationFilter} represents an aggregation filter. Aggregation filters are denying filters, i.e. if some filter
 * denies aggregation that aggregation is denied regardless of outcome of other filters.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationFilter {
    /**
     * Denies or allows aggregation of measurement with specified combination of scope and metric.
     *
     * @param scope  scope name
     * @param metric metric name or null if aggregation of stack metrics in scopes is performed
     * @return true if aggregation is allowed
     */
    boolean deny(IScopeName scope, IMetricName metric);
}
