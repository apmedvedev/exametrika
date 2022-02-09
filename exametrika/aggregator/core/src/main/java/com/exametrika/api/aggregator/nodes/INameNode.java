/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.schema.INameNodeSchema;


/**
 * The {@link INameNode} represents a name node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INameNode extends IAggregationNode {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    INameNodeSchema getSchema();

    /**
     * Returns node metric location.
     *
     * @return node metric location
     */
    @Override
    IMetricName getMetric();

    /**
     * Is aggregation in derived nodes of this node blocked?
     *
     * @return true if aggregation in derived if this node is blocked
     */
    boolean isDerivedAggregationBlocked();

    /**
     * Returns scope parent.
     *
     * @return scope parent or null if parent is not set
     */
    INameNode getScopeParent();

    /**
     * Returns scope children.
     *
     * @return scope children
     */
    Iterable<INameNode> getScopeChildren();

    /**
     * Adds new scope child.
     *
     * @param child scope child
     */
    void addScopeChild(INameNode child);

    /**
     * Returns metric parent.
     *
     * @return metric parent or null if parent is not set
     */
    INameNode getMetricParent();

    /**
     * Returns metric children.
     *
     * @return metric children
     */
    Iterable<INameNode> getMetricChildren();

    /**
     * Adds new metric child.
     *
     * @param child metric child
     */
    void addMetricChild(INameNode child);
}
