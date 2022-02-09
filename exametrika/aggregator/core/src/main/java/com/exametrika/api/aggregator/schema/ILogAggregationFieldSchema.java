/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;


/**
 * The {@link ILogAggregationFieldSchema} represents a schema for aggregation log field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ILogAggregationFieldSchema extends IAggregationFieldSchema {
    @Override
    LogAggregationFieldSchemaConfiguration getConfiguration();

    /**
     * Returns full text document schema.
     *
     * @return full text document schema or null if log does not have full text index
     */
    IDocumentSchema getDocumentSchema();
}
