/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.fields;

import java.util.List;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterable;
import com.exametrika.api.aggregator.schema.IPeriodAggregationFieldSchema;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;


/**
 * The {@link IPeriodAggregationField} represents an aggregation field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IPeriodAggregationField extends IAggregationField {
    @Override
    IPeriodAggregationFieldSchema getSchema();

    /**
     * Returns aggregation log.
     *
     * @return aggregation log or null if field does not have aggregation log
     */
    ILogAggregationField getLog();

    /**
     * Returns current aggregated value.
     *
     * @return current aggregated value
     */
    @Override
    IComponentValue get();

    /**
     * Returns iterable on aggregation records in current period.
     *
     * @return iterable on aggregation records in current period or null if field does not have aggregation log
     */
    IAggregationIterable getPeriodRecords();

    /**
     * Aggregates value.
     *
     * @param value   aggregation value
     * @param context aggregate context
     */
    void aggregate(IComponentValue value, IAggregationContext context);

    /**
     * Called when period is closed.
     *
     * @param period closed period
     * @return list of secondary measurements or null if secondary measurements are not produced
     */
    List<Measurement> onPeriodClosed(IPeriod period);

    /**
     * Adds new aggregation record for non-aggregating period.
     *
     * @param value  aggregation value
     * @param time   end aggregation time
     * @param period aggregation period
     */
    void add(IComponentValue value, long time, long period);
}
