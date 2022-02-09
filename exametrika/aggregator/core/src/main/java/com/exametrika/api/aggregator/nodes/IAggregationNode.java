/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link IAggregationNode} represents an aggregation node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAggregationNode extends IPeriodNode {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    IAggregationNodeSchema getSchema();

    /**
     * Returns componentType of node.
     *
     * @return componentType of node
     */
    String getComponentType();

    /**
     * Returns type of node used in representations.
     *
     * @return node type
     */
    String getNodeType();

    /**
     * Returns flags.
     *
     * @return flags
     */
    int getFlags();

    /**
     * Is node derived from end measurement node in current period?
     *
     * @return true if node is derived
     */
    boolean isDerived();

    /**
     * Returns aggregation field.
     *
     * @return aggregation field
     */
    IAggregationField getAggregationField();

    /**
     * Returns metadata.
     *
     * @return metadata or null if metadata are not set
     */
    JsonObject getMetadata();
}
