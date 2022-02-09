/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;


/**
 * The {@link IPeriodAggregationFieldSchema} represents a schema for aggregation field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodAggregationFieldSchema extends IAggregationFieldSchema {
    @Override
    PeriodAggregationFieldSchemaConfiguration getConfiguration();

    /**
     * Returns index of aggregation log reference field.
     *
     * @return index of aggregation log reference field or -1 if field does not have corresponding aggregation log
     */
    int getLogReferenceFieldIndex();

    /**
     * Returns index of analysis field.
     *
     * @return index of analysis field or -1 if field does not have corresponding analysis field
     */
    int getAnalysisFieldIndex();

    /**
     * Returns schema of aggregation log field.
     *
     * @return schema of aggregation log field or null if metric type does not have aggregation log.
     */
    IFieldSchema getAggregationLog();

    /**
     * Returns aggregator for non-stack field.
     *
     * @return aggregator for non-stack field
     */
    IComponentAggregator getAggregator();
}
