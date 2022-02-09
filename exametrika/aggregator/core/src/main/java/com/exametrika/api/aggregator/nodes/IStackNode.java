/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;


/**
 * The {@link IStackNode} represents a stack node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStackNode extends IAggregationNode {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    IStackNodeSchema getSchema();

    /**
     * Returns node metric location.
     *
     * @return node metric location
     */
    @Override
    ICallPath getMetric();

    /**
     * Returns transaction segment stack root.
     *
     * @return transaction segment stack root
     */
    IEntryPointNode getRoot();

    /**
     * Returns transaction root.
     *
     * @return transaction root or null if transaction root is not set
     */
    IEntryPointNode getTransactionRoot();

    /**
     * Returns parent.
     *
     * @return parent or null if parent is not set
     */
    IStackNode getParent();

    /**
     * Returns children.
     *
     * @return children
     */
    Iterable<IStackNode> getChildren();

    /**
     * Adds new child.
     *
     * @param child child
     */
    void addChild(IStackNode child);

    /**
     * Returns dependent name nodes.
     *
     * @return dependent name nodes
     */
    Iterable<Dependency<IStackNameNode>> getDependents();

    /**
     * Adds new dependent name node.
     *
     * @param dependent dependent
     * @param total     if true dependency is inherent and total, if false - only inherent
     */
    void addDependent(IStackNameNode dependent, boolean total);
}
