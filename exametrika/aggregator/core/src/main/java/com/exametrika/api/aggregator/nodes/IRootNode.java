/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriodNode;


/**
 * The {@link IRootNode} represents a root node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRootNode extends IPeriodNode {
    /**
     * Returns end name nodes.
     *
     * @return end name nodes
     */
    Iterable<INameNode> getNameNodes();

    /**
     * Adds end name node.
     *
     * @param node end name node
     */
    void addNameNode(INameNode node);

    /**
     * Returns background roots.
     *
     * @return background roots
     */
    Iterable<IBackgroundRootNode> getBackgroundRoots();

    /**
     * Adds background root.
     *
     * @param root background root
     */
    void addBackgroundRoot(IBackgroundRootNode root);

    /**
     * Returns transaction roots.
     *
     * @return transaction roots
     */
    Iterable<IPrimaryEntryPointNode> getTransactionRoots();

    /**
     * Adds transaction root.
     *
     * @param root transaction root
     */
    void addTransactionRoot(IPrimaryEntryPointNode root);

    /**
     * Returns secondary entry points.
     *
     * @return secondary entry points
     */
    Iterable<ISecondaryEntryPointNode> getSecondaryEntryPoints();

    /**
     * Adds secondary entry point.
     *
     * @param value secondary entry point
     */
    void addSecondaryEntryPoint(ISecondaryEntryPointNode value);

    /**
     * Returns stack log nodes.
     *
     * @return children
     */
    Iterable<IAggregationNode> getLogs();

    /**
     * Adds new stack log node.
     *
     * @param node stack log node
     */
    void addLog(IAggregationNode node);

    /**
     * Returns derived roots.
     *
     * @return derived roots
     */
    Iterable<IAggregationNode> getDerivedRoots();

    /**
     * Adds derived root.
     *
     * @param root derived root
     */
    void addDerivedRoot(IAggregationNode root);
}
