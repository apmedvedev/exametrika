/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link ISecondaryEntryPointNode} represents a secondary entry point node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISecondaryEntryPointNode extends IEntryPointNode {
    public enum CombineType {
        STACK,
        TRANSACTION,
        NODE,
        ALL
    }

    /**
     * Is entry point connected with synchronous or asynchronous exit point?
     *
     * @return true if entry point is connected with synchronous exit point
     */
    boolean isSync();

    /**
     * Is entry point an inner entry point of its scope?
     *
     * @return true if entry point is an inner entry point of its scope
     */
    boolean isScopeInner();

    /**
     * Returns parent exit point node.
     *
     * @return parent exit point node or null if exit point is not resolved
     */
    IIntermediateExitPointNode getParentExitPoint();
}
