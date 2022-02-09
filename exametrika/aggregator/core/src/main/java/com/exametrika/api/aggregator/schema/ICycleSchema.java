/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSpaceSchema;


/**
 * The {@link ICycleSchema} represents a schema for cycle of particular space and period.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICycleSchema extends INodeSpaceSchema {
    String TYPE = "period";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    PeriodSchemaConfiguration getConfiguration();

    /**
     * Returns index of cycle schema in list of cycle schemas of {@link IPeriodSpaceSchema}.
     *
     * @return index of cycle schema
     */
    int getIndex();

    /**
     * Returns space schema.
     *
     * @return space schema
     */
    @Override
    IPeriodSpaceSchema getParent();

    /**
     * Returns schema of period cycle root node.
     *
     * @return schema of period cycle root node or null if period cycle root node is not set
     */
    INodeSchema getCyclePeriodRootNode();

    /**
     * Returns current cycle.
     *
     * @return current cycle or null if current cycle does not exist
     */
    IPeriodCycle getCurrentCycle();

    /**
     * Returns iterator for all cycles starting from current to previous.
     *
     * @return iterator for all cycles starting from current to previous in reverse creation order
     */
    Iterable<IPeriodCycle> getCycles();

    /**
     * Finds cycle for a given moment of time.
     *
     * @param time selection time
     * @return cycle or null if cycle for a given moment of time is not found
     */
    IPeriodCycle findCycle(long time);

    /**
     * Finds cycle by identifier.
     *
     * @param cycleId cycle identifier
     * @return cycle or null if cycle for a given identifier is not found
     */
    IPeriodCycle findCycleById(String cycleId);

    /**
     * Finds aggregation node schema by component type.
     *
     * @param componentType component type
     * @return aggregation node schema or null if aggregation node schema for specified component type is not found
     */
    IAggregationNodeSchema findAggregationNode(String componentType);
}
