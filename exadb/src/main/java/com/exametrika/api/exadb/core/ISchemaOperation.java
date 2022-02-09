/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import com.exametrika.common.rawdb.RawRollbackException;


/**
 * The {@link ISchemaOperation} represents an operation executed in schema transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISchemaOperation {
    /**
     * Returns estimated operation size.
     *
     * @return estimated operation size
     */
    int getSize();

    /**
     * Runs operation in transaction.
     *
     * @param transaction enclosing transaction
     * @throws RawRollbackException (or any other exception) if transaction is rolled back
     */
    void run(ISchemaTransaction transaction);
}
