/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;


/**
 * The {@link IObjectNode} represents a object node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IObjectNode extends INode {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    IObjectNodeSchema getSchema();

    /**
     * Returns node space.
     *
     * @return node space
     */
    @Override
    IObjectSpace getSpace();

    /**
     * Returns node's primary key.
     *
     * @return node's primary key or null if node does not have primary key
     */
    Object getKey();
}
