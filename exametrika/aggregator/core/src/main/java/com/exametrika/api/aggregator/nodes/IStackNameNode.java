/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link IStackNameNode} represents a stack name node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStackNameNode extends INameNode {
    /**
     * Returns node's dependencies.
     *
     * @return node's dependencies
     */
    Iterable<Dependency<IStackNode>> getDependencies();
}
