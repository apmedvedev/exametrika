/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.common.rawdb.IRawTransaction;


/**
 * The {@link ITransactionProvider} represents a provider of current transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITransactionProvider {
    /**
     * Returns current low-level raw database transaction.
     *
     * @return current low-level database transaction
     */
    IRawTransaction getRawTransaction();

    /**
     * Returns current transaction.
     *
     * @return current transaction
     */
    ITransaction getTransaction();
}
