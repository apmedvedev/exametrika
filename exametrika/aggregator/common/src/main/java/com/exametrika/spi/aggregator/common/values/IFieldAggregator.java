/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;


/**
 * The {@link IFieldAggregator} represents a field aggregator.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldAggregator {
    /**
     * Aggregates specified field values in given fields.
     *
     * @param fields      fields to add to
     * @param fieldsToAdd fields to add
     * @param context     aggregate context
     */
    void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context);
}
