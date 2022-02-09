/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;


/**
 * The {@link IPeriodSpaceSchema} represents a schema for periodic node space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodSpaceSchema extends ISpaceSchema {
    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    PeriodSpaceSchemaConfiguration getConfiguration();

    /**
     * Returns parent.
     *
     * @return parent
     */
    @Override
    IDomainSchema getParent();

    /**
     * Returns list of cycle schemas in the same order as order of periods in space schema configuration.
     *
     * @return list of cycle schemas
     */
    List<ICycleSchema> getCycles();

    /**
     * Finds cycle schema by period name.
     *
     * @param periodName name of period
     * @return cycle schema or null if cycle is not found
     */
    ICycleSchema findCycle(String periodName);

    /**
     * Finds cycle schema by period alias.
     *
     * @param periodAlias alias of period
     * @return cycle schema or null if cycle is not found
     */
    ICycleSchema findCycleByAlias(String periodAlias);

    /**
     * Finds aggregation node schema by component type. Returns first occurence of schema in nearest aggregation period
     * type, starting from first. Non-aggregating period is not examined.
     *
     * @param componentType component type
     * @return aggregation node schema or null if aggregation node schema for specified component type is not found
     */
    IAggregationNodeSchema findAggregationNode(String componentType);
}
