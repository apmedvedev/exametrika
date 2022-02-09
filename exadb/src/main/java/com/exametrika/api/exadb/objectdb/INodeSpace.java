/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.core.ITransaction;


/**
 * The {@link INodeSpace} represents a node space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeSpace {
    /**
     * Returns node cache of space.
     *
     * @return node cache of space
     */
    public INodeCache getNodeCache();

    /**
     * Returns current transaction.
     *
     * @return current transaction
     */
    ITransaction getTransaction();
}
