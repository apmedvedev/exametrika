/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link IEntryPointNode} represents an entry point node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IEntryPointNode extends IStackNode {
    /**
     * Returns scope parent.
     *
     * @return scope parent or null if not does not have scope parent
     */
    IEntryPointNode getScopeParent();

    /**
     * Returns scope children.
     *
     * @return scope children
     */
    Iterable<IEntryPointNode> getScopeChildren();

    /**
     * Adds new scope child.
     *
     * @param child scope child
     */
    void addScopeChild(IEntryPointNode child);

    /**
     * Returns transaction segment exit points.
     *
     * @return transaction segment exit points
     */
    Iterable<IExitPointNode> getExitPoints();

    /**
     * Adds new exit point.
     *
     * @param node exit point node
     */
    void addExitPoint(IExitPointNode node);

    /**
     * Returns stack log nodes.
     *
     * @return children
     */
    Iterable<IStackLogNode> getLogs();

    /**
     * Adds new stack log node.
     *
     * @param node stack log node
     */
    void addLog(IStackLogNode node);
}
