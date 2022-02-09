/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link ILogRootNode} represents a log root node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILogRootNode extends IRootNode {
    long getBlobStoreFreeSpace();
}
