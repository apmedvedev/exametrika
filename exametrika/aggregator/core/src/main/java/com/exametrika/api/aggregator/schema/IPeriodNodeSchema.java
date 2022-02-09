/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;


/**
 * The {@link IPeriodNodeSchema} represents a schema for period node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodNodeSchema extends INodeSchema {
    @Override
    PeriodNodeSchemaConfiguration getConfiguration();

    @Override
    ICycleSchema getParent();

    /**
     * Returns schema of node with the same name from previous period.
     *
     * @return schema of node with the same name from previous period or null if schema is not found
     */
    IPeriodNodeSchema getPreviousPeriodNode();

    /**
     * Returns schema of node with the same name from next period.
     *
     * @return schema of node with the same name from next period or null if schema is not found
     */
    IPeriodNodeSchema getNextPeriodNode();
}
