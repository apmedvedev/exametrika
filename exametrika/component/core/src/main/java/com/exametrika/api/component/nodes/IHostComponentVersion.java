/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IHostComponentVersion} represents a host component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHostComponentVersion extends IAgentComponentVersion {
    /**
     * Returns nodes.
     *
     * @return nodes
     */
    Iterable<INodeComponent> getNodes();
}
