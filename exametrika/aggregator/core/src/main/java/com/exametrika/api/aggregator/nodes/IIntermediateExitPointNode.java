/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link IIntermediateExitPointNode} represents an intermediate exit point node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIntermediateExitPointNode extends IExitPointNode {
    /**
     * Is exit point leaf or parent?
     *
     * @return true if exit point is leaf
     */
    boolean isLeaf();

    /**
     * Is exit point synchronous or asynchronous?
     *
     * @return true if exit point is synchronous
     */
    boolean isSync();

    /**
     * Is exitpoint an inner exitpoint of its scope?
     *
     * @return true if exit point is an inner exit point of its scope
     */
    boolean isScopeInner();

    /**
     * Returns child entry point node.
     *
     * @return child entry point node or null if entry point is not resolved
     */
    ISecondaryEntryPointNode getChildEntryPoint();
}
