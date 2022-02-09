/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.schema.IStackLogNodeSchema;


/**
 * The {@link IStackLogNode} represents a stack log node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStackLogNode extends IAggregationNode {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    IStackLogNodeSchema getSchema();

    /**
     * Returns node metric location.
     *
     * @return node metric location
     */
    @Override
    ICallPath getMetric();

    /**
     * Returns main node of this stack log node.
     *
     * @return main node of this stack log node
     */
    IStackNode getMainNode();
}
