/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import com.exametrika.spi.exadb.objectdb.NodeObject;


/**
 * The {@link INodeLoader} represents a loader of nodes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeLoader {
    /**
     * Loads node by identifier.
     *
     * @param id     node identifier
     * @param object node object
     * @return node
     */
    Node loadNode(long id, NodeObject object);
}
