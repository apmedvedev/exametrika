/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;


/**
 * The {@link IAggregationLogFilter} represents an aggregation log filter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationLogFilter {
    /**
     * Allows or denies aggregation of specified log record in specified field.
     *
     * @param field     aggregation field
     * @param logRecord log record
     * @return true if aggregation is allowed
     */
    boolean allow(IPeriodAggregationField field, IAggregationRecord logRecord);
}
